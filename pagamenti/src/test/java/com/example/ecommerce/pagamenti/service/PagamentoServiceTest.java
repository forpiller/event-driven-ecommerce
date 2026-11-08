package com.example.ecommerce.pagamenti.service;

import com.example.ecommerce.pagamenti.repository.OutboxEventRepository;
import com.example.ecommerce.pagamenti.repository.PagamentoRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato", "pagamento.eseguito", "pagamento.fallito", "pagamento.rimborsato"})
@DirtiesContext
class PagamentoServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("pagamenti_db")
            .withUsername("pagamenti")
            .withPassword("pagamenti");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    GatewayBancaClient gatewayBancaClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    PagamentoService pagamentoService;

    @Autowired
    PagamentoRepository pagamentoRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Test
    void circuitBreakerParteClosed() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("gateway-banca");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
