package com.example.ecommerce.pagamenti.service;

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

    // ON CONFLICT DO NOTHING: l'INSERT non lancia mai eccezioni su duplicato — restituisce
    // semplicemente 0 righe inserite. Nessun REQUIRES_NEW: gira nella tx del chiamante.
    // Funziona correttamente per Kafka consumers (elaborazione sequenziale per partizione).
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
