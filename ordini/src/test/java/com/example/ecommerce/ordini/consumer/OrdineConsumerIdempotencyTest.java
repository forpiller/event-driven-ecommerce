package com.example.ecommerce.ordini.consumer;

import com.example.ecommerce.ordini.domain.CreaOrdineRequest;
import com.example.ecommerce.ordini.entity.OrdineStato;
import com.example.ecommerce.ordini.event.MerceRiservata;
import com.example.ecommerce.ordini.repository.OrdineRepository;
import com.example.ecommerce.ordini.service.OrdineService;
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
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato", "pagamento.eseguito", "merce.riservata", "pagamento.rimborsato", "pagamento.fallito"})
@DirtiesContext
class OrdineConsumerIdempotencyTest {

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

    @Autowired
    EventiProcessor eventiProcessor;

    private UUID creaOrdineConStatoPagato() {
        CreaOrdineRequest req = new CreaOrdineRequest();
        req.setProdottoId("PROD-1");
        req.setQuantita(1);
        req.setImportoTotale(new BigDecimal("10.00"));
        UUID id = ordineService.creaOrdine(req).getId();
        ordineService.aggiornaStato(id, OrdineStato.PAGATO);
        return id;
    }

    @Test
    void onMerceRiservata_stessoEventoDueVolte_confermataUnaVolta() {
        UUID ordineId = creaOrdineConStatoPagato();
        UUID eventId = UUID.randomUUID();
        MerceRiservata evento = new MerceRiservata(eventId, ordineId, "PROD-1", 1);
        eventiProcessor.onMerceRiservata(evento);
        eventiProcessor.onMerceRiservata(evento); // duplicato
        assertThat(ordineRepository.findById(ordineId).orElseThrow().getStato()).isEqualTo(OrdineStato.CONFERMATO);
    }
}
