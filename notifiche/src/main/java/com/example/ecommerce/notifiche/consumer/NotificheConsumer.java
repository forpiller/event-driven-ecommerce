package com.example.ecommerce.notifiche.consumer;

import com.example.ecommerce.notifiche.event.MerceRiservata;
import com.example.ecommerce.notifiche.event.PagamentoFallito;
import com.example.ecommerce.notifiche.event.PagamentoRimborsato;
import com.example.ecommerce.notifiche.service.NotificheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificheConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificheConsumer.class);

    private final NotificheService notificheService;
    private final ObjectMapper objectMapper;

    public NotificheConsumer(NotificheService notificheService, ObjectMapper objectMapper) {
        this.notificheService = notificheService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "merce.riservata", groupId = "notifiche")
    public void onMerceRiservata(String payload) {
        try {
            MerceRiservata event = objectMapper.readValue(payload, MerceRiservata.class);
            log.info("Ricevuto MerceRiservata: eventId={}, ordineId={}", event.getEventId(), event.getOrdineId());
            notificheService.notificaConferma(event.getEventId(), event.getOrdineId());
        } catch (Exception e) {
            log.error("Errore processing MerceRiservata: {}", e.getMessage(), e);
            throw new RuntimeException("Errore processing MerceRiservata", e);
        }
    }

    @KafkaListener(topics = "pagamento.fallito", groupId = "notifiche")
    public void onPagamentoFallito(String payload) {
        try {
            PagamentoFallito event = objectMapper.readValue(payload, PagamentoFallito.class);
            log.info("Ricevuto PagamentoFallito: eventId={}, ordineId={}", event.getEventId(), event.getOrdineId());
            notificheService.notificaFallimento(event.getEventId(), event.getOrdineId(), event.getMotivo());
        } catch (Exception e) {
            log.error("Errore processing PagamentoFallito: {}", e.getMessage(), e);
            throw new RuntimeException("Errore processing PagamentoFallito", e);
        }
    }

    @KafkaListener(topics = "pagamento.rimborsato", groupId = "notifiche")
    public void onPagamentoRimborsato(String payload) {
        try {
            PagamentoRimborsato event = objectMapper.readValue(payload, PagamentoRimborsato.class);
            log.info("Ricevuto PagamentoRimborsato: eventId={}, ordineId={}", event.getEventId(), event.getOrdineId());
            notificheService.notificaAnnullamento(event.getEventId(), event.getOrdineId());
        } catch (Exception e) {
            log.error("Errore processing PagamentoRimborsato: {}", e.getMessage(), e);
            throw new RuntimeException("Errore processing PagamentoRimborsato", e);
        }
    }
}
