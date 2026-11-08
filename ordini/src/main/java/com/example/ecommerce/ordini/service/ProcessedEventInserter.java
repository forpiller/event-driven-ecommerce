package com.example.ecommerce.ordini.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessedEventInserter {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventInserter.class);

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventInserter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ON CONFLICT DO NOTHING: non lancia mai eccezioni su duplicato — restituisce 0 righe.
    // Nessun @Transactional: gira nella transazione del chiamante.
    public boolean tryInsert(UUID eventId) {
        int rows = jdbcTemplate.update(
                "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING",
                eventId, Timestamp.valueOf(LocalDateTime.now())
        );
        if (rows == 0) {
            log.debug("Evento {} già processato, skip", eventId);
        }
        return rows > 0;
    }
}
