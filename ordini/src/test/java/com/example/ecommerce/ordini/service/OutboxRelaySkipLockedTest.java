package com.example.ecommerce.ordini.service;

import com.example.ecommerce.ordini.entity.OutboxEvent;
import com.example.ecommerce.ordini.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ordine.creato"})
@DirtiesContext
class OutboxRelaySkipLockedTest {

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
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxRelay outboxRelay;

    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("relay-skip-locked-test", "false", embeddedKafka);
        consumerProps.put("auto.offset.reset", "earliest");
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer = cf.createConsumer();
        consumer.subscribe(List.of("ordine.creato"));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
        outboxEventRepository.deleteAll();
    }

    @Test
    void dueRelayConcorrenti_pubblicanoEventoEsattamenteUnaVolta() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("Ordine");
        event.setAggregateId(UUID.randomUUID().toString());
        event.setEventType("OrdineCreato");
        event.setPayload("{\"eventId\":\"" + UUID.randomUUID() + "\",\"ordineId\":\"" + UUID.randomUUID() + "\",\"prodottoId\":\"PROD-X\",\"quantita\":1,\"importoTotale\":10.00}");
        event.setPublished(false);
        event.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.saveAndFlush(event);

        AtomicInteger count = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                startLatch.await();
                outboxRelay.relay();
            } catch (Exception e) {
                // skip interrupted
            } finally {
                doneLatch.countDown();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
            records.forEach(r -> count.incrementAndGet());
            assertThat(count.get()).isGreaterThanOrEqualTo(1);
        });

        Thread.sleep(600);
        ConsumerRecords<String, String> remaining = consumer.poll(Duration.ofMillis(500));
        remaining.forEach(r -> count.incrementAndGet());

        assertThat(count.get()).isEqualTo(1);
    }
}
