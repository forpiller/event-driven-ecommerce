package com.example.ecommerce.pagamenti.consumer;

import com.example.ecommerce.pagamenti.event.OrdineCreato;
import com.example.ecommerce.pagamenti.service.PagamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrdineConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdineConsumer.class);

    private final PagamentoService pagamentoService;
    private final ObjectMapper objectMapper;

    public OrdineConsumer(PagamentoService pagamentoService, ObjectMapper objectMapper) {
        this.pagamentoService = pagamentoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ordine.creato", groupId = "pagamenti")
    public void onOrdineCreato(String payload) {
        try {
            OrdineCreato evento = objectMapper.readValue(payload, OrdineCreato.class);
            log.info("Ricevuto OrdineCreato: eventId={}, ordineId={}", evento.getEventId(), evento.getOrdineId());
            pagamentoService.addebita(
                    evento.getEventId(),
                    evento.getOrdineId(),
                    evento.getImportoTotale(),
                    evento.getProdottoId(),
                    evento.getQuantita()
            );
        } catch (Exception e) {
            log.error("Errore processing OrdineCreato: {}", e.getMessage(), e);
            throw new RuntimeException("Errore processing OrdineCreato", e);
        }
    }
}
