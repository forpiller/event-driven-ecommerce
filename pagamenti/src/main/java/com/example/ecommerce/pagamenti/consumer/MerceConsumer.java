package com.example.ecommerce.pagamenti.consumer;

import com.example.ecommerce.pagamenti.event.MerceNonDisponibile;
import com.example.ecommerce.pagamenti.service.PagamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MerceConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerceConsumer.class);

    private final PagamentoService pagamentoService;
    private final ObjectMapper objectMapper;

    public MerceConsumer(PagamentoService pagamentoService, ObjectMapper objectMapper) {
        this.pagamentoService = pagamentoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "merce.non.disponibile", groupId = "pagamenti")
    public void onMerceNonDisponibile(String message) {
        try {
            MerceNonDisponibile evento = objectMapper.readValue(message, MerceNonDisponibile.class);
            log.info("Ricevuto MerceNonDisponibile: eventId={}, ordineId={}", evento.getEventId(), evento.getOrdineId());
            pagamentoService.rimborsa(evento.getEventId(), evento.getOrdineId());
        } catch (Exception e) {
            log.error("Errore processing merce.non.disponibile: {}", e.getMessage(), e);
        }
    }
}
