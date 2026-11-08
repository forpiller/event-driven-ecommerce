package com.example.ecommerce.magazzino.service;

import com.example.ecommerce.magazzino.domain.StockDto;
import com.example.ecommerce.magazzino.entity.Prodotto;
import com.example.ecommerce.magazzino.event.MerceNonDisponibile;
import com.example.ecommerce.magazzino.event.MerceRiservata;
import com.example.ecommerce.magazzino.repository.ProdottoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class MagazzinoService {

    private static final Logger log = LoggerFactory.getLogger(MagazzinoService.class);

    private static final String TOPIC_MERCE_RISERVATA = "merce.riservata";
    private static final String TOPIC_MERCE_NON_DISPONIBILE = "merce.non.disponibile";

    private final ProdottoRepository prodottoRepository;
    private final ProcessedEventInserter processedEventInserter;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public MagazzinoService(ProdottoRepository prodottoRepository,
                             ProcessedEventInserter processedEventInserter,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.prodottoRepository = prodottoRepository;
        this.processedEventInserter = processedEventInserter;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void riserva(UUID eventId, UUID ordineId, String prodottoId, int quantita) {
        if (!processedEventInserter.tryInsert(eventId)) {
            return;
        }

        Prodotto prodotto = prodottoRepository.findWithLockById(prodottoId).orElse(null);
        if (prodotto == null) {
            log.warn("Prodotto non trovato: {} — pubblicato MerceNonDisponibile", prodottoId);
            MerceNonDisponibile nf = new MerceNonDisponibile(UUID.randomUUID(), ordineId, prodottoId, quantita);
            pubblicaEvento(TOPIC_MERCE_NON_DISPONIBILE, ordineId.toString(), nf);
            return;
        }

        if (prodotto.getQuantita() >= quantita) {
            prodotto.setQuantita(prodotto.getQuantita() - quantita);
            prodottoRepository.save(prodotto);

            MerceRiservata evento = new MerceRiservata(UUID.randomUUID(), ordineId, prodottoId, quantita);
            pubblicaEvento(TOPIC_MERCE_RISERVATA, ordineId.toString(), evento);
        } else {
            MerceNonDisponibile evento = new MerceNonDisponibile(UUID.randomUUID(), ordineId, prodottoId, quantita);
            pubblicaEvento(TOPIC_MERCE_NON_DISPONIBILE, ordineId.toString(), evento);
        }
    }

    public StockDto getStock(String prodottoId) {
        Prodotto prodotto = prodottoRepository.findById(prodottoId).orElse(null);
        StockDto dto = new StockDto();
        dto.setProdottoId(prodottoId);
        dto.setQuantita(prodotto != null ? prodotto.getQuantita() : 0);
        return dto;
    }

    @Transactional
    public void setStock(String prodottoId, int quantita) {
        Prodotto prodotto = prodottoRepository.findById(prodottoId).orElseGet(() -> {
            Prodotto nuovo = new Prodotto();
            nuovo.setId(prodottoId);
            nuovo.setNome(prodottoId);
            return nuovo;
        });
        prodotto.setQuantita(quantita);
        prodottoRepository.save(prodotto);
    }

    private void pubblicaEvento(String topic, String key, Object evento) {
        try {
            String json = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, key, json);
            log.info("Pubblicato su {}: key={}", topic, key);
        } catch (Exception e) {
            log.error("Errore pubblicazione evento su {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Errore pubblicazione evento su " + topic, e);
        }
    }
}
