package com.example.ecommerce.pagamenti.consumer;

import com.example.ecommerce.pagamenti.repository.PagamentoRepository;
import com.example.ecommerce.pagamenti.service.PagamentoService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Nessun @Transactional sul test: ogni addebita() gira nella propria transazione autonoma,
// come avviene in produzione. Con @Transactional + REQUIRES_NEW, Hibernate 6 propaga
// rollbackOnly dalla sub-tx verso l'outer tx del test causando UnexpectedRollbackException.
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato", "pagamento.eseguito", "pagamento.fallito", "pagamento.rimborsato"})
@DirtiesContext
class OrdineConsumerIdempotencyTest {

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
    PagamentoService pagamentoService;

    @Autowired
    PagamentoRepository pagamentoRepository;

    @Test
    void consumerStessoEventoRicevutoDueVolte_processoUnaVolta() {
        UUID ordineId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        pagamentoService.addebita(eventId, ordineId, new BigDecimal("29.99"), "PROD-1", 1);
        pagamentoService.addebita(eventId, ordineId, new BigDecimal("29.99"), "PROD-1", 1); // duplicato

        assertThat(pagamentoRepository.findByOrdineId(ordineId)).hasSize(1);
    }
}
