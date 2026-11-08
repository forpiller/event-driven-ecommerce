package com.example.ecommerce.it;

import com.example.ecommerce.magazzino.MagazzinoApplication;
import com.example.ecommerce.magazzino.service.MagazzinoService;
import com.example.ecommerce.notifiche.NotificheApplication;
import com.example.ecommerce.ordini.OrdiniApplication;
import com.example.ecommerce.ordini.domain.OrdineDto;
import com.example.ecommerce.ordini.entity.OrdineStato;
import com.example.ecommerce.ordini.repository.OrdineRepository;
import com.example.ecommerce.pagamenti.PagamentiApplication;
import com.example.ecommerce.pagamenti.entity.PagamentoStato;
import com.example.ecommerce.pagamenti.repository.PagamentoRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSagaIntegrationTest {

    static Network network;
    static KafkaContainer kafka;
    static PostgreSQLContainer<?> ordiniDb;
    static PostgreSQLContainer<?> pagamentiDb;
    static PostgreSQLContainer<?> magazzinoDb;
    static PostgreSQLContainer<?> notificheDb;
    static ConfigurableApplicationContext ordiniCtx;
    static ConfigurableApplicationContext pagamentiCtx;
    static ConfigurableApplicationContext magazzinoCtx;
    static ConfigurableApplicationContext notificheCtx;
    static int ordiniPort;
    static int pagamentiPort;

    @BeforeAll
    @SuppressWarnings("resource")
    static void startAll() throws Exception {
        network = Network.newNetwork();
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                .withNetwork(network);
        ordiniDb = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("ordini_db").withUsername("ordini").withPassword("ordini");
        pagamentiDb = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("pagamenti_db").withUsername("pagamenti").withPassword("pagamenti");
        magazzinoDb = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("magazzino_db").withUsername("magazzino").withPassword("magazzino");
        notificheDb = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("notifiche_db").withUsername("notifiche").withPassword("notifiche");

        kafka.start();
        ordiniDb.start();
        pagamentiDb.start();
        magazzinoDb.start();
        notificheDb.start();

        ordiniPort = findFreePort();
        pagamentiPort = findFreePort();
        int magazzinoPort = findFreePort();
        int notifichePort = findFreePort();

        String bootstrapServers = kafka.getBootstrapServers();

        // user.dir = integration-tests basedir; il parent è la root del progetto
        String projectRoot = Paths.get(System.getProperty("user.dir")).getParent().toString();

        // Le properties vengono passate come command-line args (priorità massima)
        // per sovrascrivere i valori di default nell'application.yml di ogni servizio
        ordiniCtx = new SpringApplicationBuilder(OrdiniApplication.class)
                .run(toArgs(serviceProps(ordiniDb, bootstrapServers, ordiniPort,
                        projectRoot + "/ordini/src/main/resources/db/migration")));

        // "local" profile attiva GatewayBancaSimulator; app.gateway.url punta a se stesso
        Map<String, Object> pagamentiProps = serviceProps(pagamentiDb, bootstrapServers, pagamentiPort,
                projectRoot + "/pagamenti/src/main/resources/db/migration");
        pagamentiProps.put("app.gateway.url", "http://localhost:" + pagamentiPort);
        pagamentiCtx = new SpringApplicationBuilder(PagamentiApplication.class)
                .profiles("local")
                .run(toArgs(pagamentiProps));

        magazzinoCtx = new SpringApplicationBuilder(MagazzinoApplication.class)
                .run(toArgs(serviceProps(magazzinoDb, bootstrapServers, magazzinoPort,
                        projectRoot + "/magazzino/src/main/resources/db/migration")));

        notificheCtx = new SpringApplicationBuilder(NotificheApplication.class)
                .run(toArgs(serviceProps(notificheDb, bootstrapServers, notifichePort,
                        projectRoot + "/notifiche/src/main/resources/db/migration")));
    }

    @AfterAll
    static void stopAll() {
        closeQuietly(ordiniCtx);
        closeQuietly(pagamentiCtx);
        closeQuietly(magazzinoCtx);
        closeQuietly(notificheCtx);
        kafka.stop();
        ordiniDb.stop();
        pagamentiDb.stop();
        magazzinoDb.stop();
        notificheDb.stop();
        network.close();
    }

    // Scenario: magazzino senza scorte → saga di compensazione → ordine CANCELLATO, pagamento RIMBORSATO
    @Test
    void ordineConMerceEsaurita_vieneCancellato_ePagamentoRimborsato() {
        // seed: prodotto con stock 0 (nessuna scorta disponibile)
        MagazzinoService magazzinoService = magazzinoCtx.getBean(MagazzinoService.class);
        magazzinoService.setStock("PROD-OUT", 0);

        // crea ordine via REST
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prodottoId\":\"PROD-OUT\",\"quantita\":1,\"importoTotale\":9.99}";
        OrdineDto ordineDto = rt.postForObject(
                "http://localhost:" + ordiniPort + "/api/ordini",
                new HttpEntity<>(requestBody, headers),
                OrdineDto.class);

        assertThat(ordineDto).isNotNull();
        UUID ordineId = ordineDto.getId();

        // attende propagazione saga: ordine.creato → pagamento.eseguito → merce.non.disponibile
        //                           → pagamento.rimborsato → ordine CANCELLATO
        OrdineRepository ordineRepo = ordiniCtx.getBean(OrdineRepository.class);
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> ordineRepo.findById(ordineId)
                        .map(o -> o.getStato() == OrdineStato.CANCELLATO)
                        .orElse(false));

        // verifica che il pagamento risulti rimborsato
        PagamentoRepository pagRepo = pagamentiCtx.getBean(PagamentoRepository.class);
        assertThat(pagRepo.findByOrdineId(ordineId))
                .isNotEmpty()
                .first()
                .extracting(p -> p.getStato())
                .isEqualTo(PagamentoStato.RIMBORSATO);
    }

    // Scenario happy path: merce disponibile → ordine CONFERMATO
    @Test
    void ordineConMerceDisponibile_vieneConfermato() {
        MagazzinoService magazzinoService = magazzinoCtx.getBean(MagazzinoService.class);
        magazzinoService.setStock("PROD-OK", 10);

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prodottoId\":\"PROD-OK\",\"quantita\":1,\"importoTotale\":29.99}";
        OrdineDto ordineDto = rt.postForObject(
                "http://localhost:" + ordiniPort + "/api/ordini",
                new HttpEntity<>(requestBody, headers),
                OrdineDto.class);

        assertThat(ordineDto).isNotNull();
        UUID ordineId = ordineDto.getId();

        OrdineRepository ordineRepo = ordiniCtx.getBean(OrdineRepository.class);
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> ordineRepo.findById(ordineId)
                        .map(o -> o.getStato() == OrdineStato.CONFERMATO)
                        .orElse(false));

        PagamentoRepository pagRepo = pagamentiCtx.getBean(PagamentoRepository.class);
        assertThat(pagRepo.findByOrdineId(ordineId))
                .isNotEmpty()
                .first()
                .extracting(p -> p.getStato())
                .isEqualTo(PagamentoStato.ESEGUITO);
    }

    // ---- helpers ----

    // flyway.locations usa il path filesystem del singolo servizio per evitare
    // conflitti di versione quando tutti e 4 i JAR condividono classpath:db/migration
    private static Map<String, Object> serviceProps(PostgreSQLContainer<?> db,
                                                     String bootstrapServers,
                                                     int port,
                                                     String migrationDir) {
        Map<String, Object> props = new HashMap<>();
        props.put("server.port", port);
        props.put("spring.datasource.url", db.getJdbcUrl());
        props.put("spring.datasource.username", db.getUsername());
        props.put("spring.datasource.password", db.getPassword());
        props.put("spring.kafka.bootstrap-servers", bootstrapServers);
        props.put("spring.kafka.consumer.auto-offset-reset", "earliest");
        props.put("spring.flyway.locations", "filesystem:" + migrationDir);
        props.put("spring.main.allow-bean-definition-overriding", "true");
        return props;
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private static void closeQuietly(ConfigurableApplicationContext ctx) {
        if (ctx != null) {
            try { ctx.close(); } catch (Exception ignored) {}
        }
    }

    // Converte la mappa in argomenti --key=value (priorità massima in Spring Boot)
    private static String[] toArgs(Map<String, Object> props) {
        return props.entrySet().stream()
                .map(e -> "--" + e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
    }
}
