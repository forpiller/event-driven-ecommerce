package com.example.ecommerce.magazzino.config;

import com.example.ecommerce.magazzino.repository.ProdottoRepository;
import com.example.ecommerce.magazzino.service.MagazzinoService;
import com.example.ecommerce.magazzino.service.ProcessedEventInserter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class ServiceConfig {

    @Bean
    public ProcessedEventInserter processedEventInserter(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventInserter(jdbcTemplate);
    }

    @Bean
    public MagazzinoService magazzinoService(ProdottoRepository prodottoRepository,
                                              ProcessedEventInserter processedEventInserter,
                                              KafkaTemplate<String, String> kafkaTemplate,
                                              ObjectMapper objectMapper) {
        return new MagazzinoService(prodottoRepository, processedEventInserter, kafkaTemplate, objectMapper);
    }
}
