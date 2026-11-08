package com.example.ecommerce.ordini.config;

import com.example.ecommerce.ordini.consumer.EventiProcessor;
import com.example.ecommerce.ordini.repository.OrdineRepository;
import com.example.ecommerce.ordini.repository.OutboxEventRepository;
import com.example.ecommerce.ordini.service.OrdineService;
import com.example.ecommerce.ordini.service.OutboxRelay;
import com.example.ecommerce.ordini.service.ProcessedEventInserter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ServiceConfig {

    @Bean
    public OrdineService ordineService(OrdineRepository ordineRepo,
                                       OutboxEventRepository outboxRepo,
                                       ObjectMapper mapper) {
        return new OrdineService(ordineRepo, outboxRepo, mapper);
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxEventRepository outboxRepo,
                                   KafkaTemplate<String, String> kafka) {
        return new OutboxRelay(outboxRepo, kafka);
    }

    @Bean
    public ProcessedEventInserter processedEventInserter(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventInserter(jdbcTemplate);
    }

    @Bean
    public EventiProcessor eventiProcessor(OrdineService ordineService,
                                           ProcessedEventInserter processedEventInserter) {
        return new EventiProcessor(ordineService, processedEventInserter);
    }
}
