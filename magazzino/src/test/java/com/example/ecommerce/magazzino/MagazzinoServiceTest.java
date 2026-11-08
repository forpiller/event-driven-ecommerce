package com.example.ecommerce.magazzino;

import com.example.ecommerce.magazzino.entity.Prodotto;
import com.example.ecommerce.magazzino.repository.ProdottoRepository;
import com.example.ecommerce.magazzino.service.MagazzinoService;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"merce.riservata", "merce.non.disponibile"})
@DirtiesContext
class MagazzinoServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("magazzino_db")
            .withUsername("magazzino")
            .withPassword("magazzino");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MagazzinoService magazzinoService;

    @Autowired
    ProdottoRepository prodottoRepository;

    @Test
    void riserva_quantitaSufficiente_decrementaQuantita() {
        UUID ordineId = UUID.randomUUID();
        magazzinoService.setStock("PROD-1", 10);
        magazzinoService.riserva(UUID.randomUUID(), ordineId, "PROD-1", 3);
        Prodotto p = prodottoRepository.findById("PROD-1").orElseThrow();
        assertThat(p.getQuantita()).isEqualTo(7);
    }

    @Test
    void riserva_quantitaInsuffic_nonModificaQuantita() {
        UUID ordineId = UUID.randomUUID();
        magazzinoService.setStock("PROD-OUT", 0);
        magazzinoService.riserva(UUID.randomUUID(), ordineId, "PROD-OUT", 1);
        Prodotto p = prodottoRepository.findById("PROD-OUT").orElseThrow();
        assertThat(p.getQuantita()).isEqualTo(0);
    }

    @Test
    void riserva_stessoEventoRicevutoDueVolte_decrementaUnaVolta() {
        UUID eventId = UUID.randomUUID();
        UUID ordineId = UUID.randomUUID();
        magazzinoService.setStock("PROD-1", 10);
        magazzinoService.riserva(eventId, ordineId, "PROD-1", 3);
        magazzinoService.riserva(eventId, ordineId, "PROD-1", 3); // duplicato
        Prodotto p = prodottoRepository.findById("PROD-1").orElseThrow();
        assertThat(p.getQuantita()).isEqualTo(7); // decremento avvenuto una sola volta
    }
}
