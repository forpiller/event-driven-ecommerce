package com.example.ecommerce.pagamenti.service;

import com.example.ecommerce.pagamenti.entity.OutboxEvent;
import com.example.ecommerce.pagamenti.entity.Pagamento;
import com.example.ecommerce.pagamenti.entity.PagamentoStato;
import com.example.ecommerce.pagamenti.event.PagamentoEseguito;
import com.example.ecommerce.pagamenti.event.PagamentoFallito;
import com.example.ecommerce.pagamenti.event.PagamentoRimborsato;
import com.example.ecommerce.pagamenti.repository.OutboxEventRepository;
import com.example.ecommerce.pagamenti.repository.PagamentoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventInserter processedEventInserter;
    private final GatewayBancaClient gatewayBancaClient;
    private final ObjectMapper objectMapper;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                             OutboxEventRepository outboxEventRepository,
                             ProcessedEventInserter processedEventInserter,
                             GatewayBancaClient gatewayBancaClient,
                             ObjectMapper objectMapper) {
        this.pagamentoRepository = pagamentoRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventInserter = processedEventInserter;
        this.gatewayBancaClient = gatewayBancaClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void addebita(UUID eventId, UUID ordineId, BigDecimal importo, String prodottoId, int quantita) {
        // Idempotenza in tx separata: REQUIRES_NEW isola la UNIQUE violation dalla tx corrente
        if (!processedEventInserter.tryInsert(eventId)) {
            return;
        }

        // 2. Crea Pagamento in stato IN_ATTESA
        Pagamento pagamento = new Pagamento();
        pagamento.setId(UUID.randomUUID());
        pagamento.setOrdineId(ordineId);
        pagamento.setImporto(importo);
        pagamento.setStato(PagamentoStato.IN_ATTESA);
        pagamento.setCreatedAt(LocalDateTime.now());
        pagamentoRepository.save(pagamento);

        // 3. Addebita via gateway
        try {
            gatewayBancaClient.addebita(importo);
            pagamento.setStato(PagamentoStato.ESEGUITO);
            pagamentoRepository.save(pagamento);

            PagamentoEseguito evento = new PagamentoEseguito(
                    UUID.randomUUID(), ordineId, importo, prodottoId, quantita);
            salvaOutboxEvent(pagamento.getId(), "PagamentoEseguito", evento);

        } catch (GatewayBancaException e) {
            log.warn("Pagamento fallito per ordine {}: {}", ordineId, e.getMessage());
            pagamento.setStato(PagamentoStato.FALLITO);
            pagamentoRepository.save(pagamento);

            PagamentoFallito evento = new PagamentoFallito(
                    UUID.randomUUID(), ordineId, e.getMessage());
            salvaOutboxEvent(pagamento.getId(), "PagamentoFallito", evento);
        }
    }

    @Transactional
    public void rimborsa(UUID eventId, UUID ordineId) {
        if (!processedEventInserter.tryInsert(eventId)) {
            return;
        }

        Pagamento pagamento = pagamentoRepository.findFirstByOrdineId(ordineId).orElse(null);
        if (pagamento == null) {
            log.warn("Nessun pagamento trovato per ordine {}, rimborso ignorato", ordineId);
            return;
        }

        // 3. Aggiorna stato e pubblica evento rimborso
        pagamento.setStato(PagamentoStato.RIMBORSATO);
        pagamentoRepository.save(pagamento);

        PagamentoRimborsato evento = new PagamentoRimborsato(
                UUID.randomUUID(), ordineId, pagamento.getImporto());
        salvaOutboxEvent(pagamento.getId(), "PagamentoRimborsato", evento);
    }

    private void salvaOutboxEvent(UUID aggregateId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Impossibile serializzare evento " + eventType, e);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType("Pagamento");
        outbox.setAggregateId(aggregateId.toString());
        outbox.setEventType(eventType);
        outbox.setPayload(json);
        outbox.setPublished(false);
        outbox.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(outbox);
    }
}
