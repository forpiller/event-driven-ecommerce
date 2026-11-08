package com.example.ecommerce.magazzino.consumer;

import com.example.ecommerce.magazzino.event.PagamentoEseguito;
import com.example.ecommerce.magazzino.service.MagazzinoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PagamentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoConsumer.class);

    private final MagazzinoService magazzinoService;
    private final ObjectMapper objectMapper;

    public PagamentoConsumer(MagazzinoService magazzinoService, ObjectMapper objectMapper) {
        this.magazzinoService = magazzinoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "pagamento.eseguito", groupId = "magazzino")
    public void onPagamentoEseguito(String payload) {
        try {
            PagamentoEseguito evento = objectMapper.readValue(payload, PagamentoEseguito.class);
            log.info("Ricevuto PagamentoEseguito: eventId={}, ordineId={}", evento.getEventId(), evento.getOrdineId());
            magazzinoService.riserva(
                    evento.getEventId(),
                    evento.getOrdineId(),
                    evento.getProdottoId(),
                    evento.getQuantita()
            );
        } catch (Exception e) {
            log.error("Errore processing PagamentoEseguito: {}", e.getMessage(), e);
            throw new RuntimeException("Errore processing PagamentoEseguito", e);
        }
    }
}
