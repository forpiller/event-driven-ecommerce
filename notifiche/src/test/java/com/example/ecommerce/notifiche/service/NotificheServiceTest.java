package com.example.ecommerce.notifiche.service;

import com.example.ecommerce.notifiche.repository.NotificaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"merce.riservata", "pagamento.fallito", "pagamento.rimborsato"})
@DirtiesContext
class NotificheServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("notifiche_db")
            .withUsername("notifiche")
            .withPassword("notifiche");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    NotificheService notificheService;

    @Autowired
    NotificaLogRepository notificaLogRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        notificaLogRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM processed_events");
    }

    @Test
    void notificaConferma_salvaLog() {
        notificheService.notificaConferma(UUID.randomUUID(), UUID.randomUUID());
        assertThat(notificaLogRepository.findAll()).hasSize(1);
        assertThat(notificaLogRepository.findAll().get(0).getTipo()).isEqualTo("CONFERMA");
    }

    @Test
    void notificaConferma_idempotente() {
        UUID eventId = UUID.randomUUID();
        notificheService.notificaConferma(eventId, UUID.randomUUID());
        notificheService.notificaConferma(eventId, UUID.randomUUID());
        assertThat(notificaLogRepository.findAll()).hasSize(1);
    }

    @Test
    void notificaFallimento_salvaLog() {
        notificheService.notificaFallimento(UUID.randomUUID(), UUID.randomUUID(), "gateway non disponibile");
        assertThat(notificaLogRepository.findAll()).hasSize(1);
        assertThat(notificaLogRepository.findAll().get(0).getTipo()).isEqualTo("FALLIMENTO");
    }

    @Test
    void notificaAnnullamento_salvaLog() {
        notificheService.notificaAnnullamento(UUID.randomUUID(), UUID.randomUUID());
        assertThat(notificaLogRepository.findAll()).hasSize(1);
        assertThat(notificaLogRepository.findAll().get(0).getTipo()).isEqualTo("ANNULLAMENTO");
    }
}
