package com.example.ecommerce.ordini.consumer;

import com.example.ecommerce.ordini.event.MerceRiservata;
import com.example.ecommerce.ordini.event.PagamentoEseguito;
import com.example.ecommerce.ordini.event.PagamentoFallito;
import com.example.ecommerce.ordini.event.PagamentoRimborsato;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventiConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventiConsumer.class);

    private final EventiProcessor processor;
    private final ObjectMapper objectMapper;

    public EventiConsumer(EventiProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "pagamento.eseguito", groupId = "ordini")
    public void consumePagamentoEseguito(String message) {
        try {
            processor.onPagamentoEseguito(objectMapper.readValue(message, PagamentoEseguito.class));
        } catch (Exception e) {
            log.error("Errore processing pagamento.eseguito: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "merce.riservata", groupId = "ordini")
    public void consumeMerceRiservata(String message) {
        try {
            processor.onMerceRiservata(objectMapper.readValue(message, MerceRiservata.class));
        } catch (Exception e) {
            log.error("Errore processing merce.riservata: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "pagamento.rimborsato", groupId = "ordini")
    public void consumePagamentoRimborsato(String message) {
        try {
            processor.onPagamentoRimborsato(objectMapper.readValue(message, PagamentoRimborsato.class));
        } catch (Exception e) {
            log.error("Errore processing pagamento.rimborsato: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "pagamento.fallito", groupId = "ordini")
    public void consumePagamentoFallito(String message) {
        try {
            processor.onPagamentoFallito(objectMapper.readValue(message, PagamentoFallito.class));
        } catch (Exception e) {
            log.error("Errore processing pagamento.fallito: {}", e.getMessage(), e);
        }
    }
}
