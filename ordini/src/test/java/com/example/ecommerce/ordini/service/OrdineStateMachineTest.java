package com.example.ecommerce.ordini.service;

import com.example.ecommerce.ordini.domain.CreaOrdineRequest;
import com.example.ecommerce.ordini.entity.OrdineStato;
import com.example.ecommerce.ordini.repository.OrdineRepository;
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

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato"})
@DirtiesContext
class OrdineStateMachineTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ordini_db")
            .withUsername("ordini")
            .withPassword("ordini");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    OrdineService ordineService;

    @Autowired
    OrdineRepository ordineRepository;

    private UUID creaOrdine() {
        CreaOrdineRequest req = new CreaOrdineRequest();
        req.setProdottoId("PROD-1");
        req.setQuantita(1);
        req.setImportoTotale(new BigDecimal("10.00"));
        return ordineService.creaOrdine(req).getId();
    }

    @Test
    void transizioneValida_Creato_a_Pagato() {
        UUID id = creaOrdine();
        ordineService.aggiornaStato(id, OrdineStato.PAGATO);
        assertThat(ordineRepository.findById(id).orElseThrow().getStato()).isEqualTo(OrdineStato.PAGATO);
    }

    @Test
    void transizioneValida_Pagato_a_Confermato() {
        UUID id = creaOrdine();
        ordineService.aggiornaStato(id, OrdineStato.PAGATO);
        ordineService.aggiornaStato(id, OrdineStato.CONFERMATO);
        assertThat(ordineRepository.findById(id).orElseThrow().getStato()).isEqualTo(OrdineStato.CONFERMATO);
    }

    @Test
    void transizioneNonValida_Confermato_non_diventa_Cancellato() {
        UUID id = creaOrdine();
        ordineService.aggiornaStato(id, OrdineStato.PAGATO);
        ordineService.aggiornaStato(id, OrdineStato.CONFERMATO);
        // transizione non valida: CONFERMATO non può andare a CANCELLATO
        ordineService.aggiornaStato(id, OrdineStato.CANCELLATO);
        assertThat(ordineRepository.findById(id).orElseThrow().getStato()).isEqualTo(OrdineStato.CONFERMATO);
    }
}
