package com.example.ecommerce.ordini.service;

import com.example.ecommerce.ordini.domain.CreaOrdineRequest;
import com.example.ecommerce.ordini.domain.OrdineDto;
import com.example.ecommerce.ordini.repository.OrdineRepository;
import com.example.ecommerce.ordini.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato"})
@DirtiesContext
@Transactional
class OrdineServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ordini_db")
            .withUsername("ordini")
            .withPassword("ordini");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    OrdineService ordineService;

    @Autowired
    OrdineRepository ordineRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Test
    void creaOrdine_salvaOrdineEOutboxNellaStessaTransazione() {
        CreaOrdineRequest req = new CreaOrdineRequest();
        req.setProdottoId("PROD-1");
        req.setQuantita(2);
        req.setImportoTotale(new BigDecimal("49.99"));

        OrdineDto dto = ordineService.creaOrdine(req);

        assertThat(ordineRepository.findById(dto.getId())).isPresent();
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll().get(0).getEventType()).isEqualTo("OrdineCreato");
        assertThat(outboxEventRepository.findAll().get(0).isPublished()).isFalse();
    }
}
