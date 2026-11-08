package com.example.ecommerce.pagamenti.config;

import com.example.ecommerce.pagamenti.repository.OutboxEventRepository;
import com.example.ecommerce.pagamenti.repository.PagamentoRepository;
import com.example.ecommerce.pagamenti.service.GatewayBancaClient;
import com.example.ecommerce.pagamenti.service.OutboxRelay;
import com.example.ecommerce.pagamenti.service.PagamentoService;
import com.example.ecommerce.pagamenti.service.ProcessedEventInserter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class ServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public GatewayBancaClient gatewayBancaClient(RestTemplate restTemplate,
                                                   @Value("${app.gateway.url:http://localhost:8082}") String url) {
        return new GatewayBancaClient(restTemplate, url);
    }

    @Bean
    public ProcessedEventInserter processedEventInserter(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventInserter(jdbcTemplate);
    }

    @Bean
    public PagamentoService pagamentoService(PagamentoRepository pagamentoRepo,
                                              OutboxEventRepository outboxRepo,
                                              ProcessedEventInserter processedEventInserter,
                                              GatewayBancaClient gateway,
                                              ObjectMapper objectMapper) {
        return new PagamentoService(pagamentoRepo, outboxRepo, processedEventInserter, gateway, objectMapper);
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxEventRepository outboxRepo, KafkaTemplate<String, String> kafka) {
        return new OutboxRelay(outboxRepo, kafka);
    }
}
