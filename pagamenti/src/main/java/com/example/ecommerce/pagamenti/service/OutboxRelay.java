package com.example.ecommerce.pagamenti.service;

import com.example.ecommerce.pagamenti.entity.OutboxEvent;
import com.example.ecommerce.pagamenti.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final Map<String, String> TOPIC_MAP = Map.of(
            "PagamentoEseguito", "pagamento.eseguito",
            "PagamentoFallito", "pagamento.fallito",
            "PagamentoRimborsato", "pagamento.rimborsato"
    );

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relay() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublishedForUpdate();
        for (OutboxEvent event : events) {
            String topic = TOPIC_MAP.get(event.getEventType());
            if (topic == null) {
                throw new IllegalStateException("Nessun topic configurato per eventType: " + event.getEventType());
            }
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
            event.setPublished(true);
            outboxEventRepository.save(event);
            log.debug("Pubblicato evento {} su topic {}", event.getId(), topic);
        }
    }
}
