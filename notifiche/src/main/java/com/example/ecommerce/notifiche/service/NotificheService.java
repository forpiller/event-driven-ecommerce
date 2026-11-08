package com.example.ecommerce.notifiche.service;

import com.example.ecommerce.notifiche.entity.NotificaLog;
import com.example.ecommerce.notifiche.repository.NotificaLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificheService {

    private static final Logger log = LoggerFactory.getLogger(NotificheService.class);

    private final NotificaLogRepository notificaLogRepository;
    private final ProcessedEventInserter processedEventInserter;

    public NotificheService(NotificaLogRepository notificaLogRepository,
                             ProcessedEventInserter processedEventInserter) {
        this.notificaLogRepository = notificaLogRepository;
        this.processedEventInserter = processedEventInserter;
    }

    @Transactional
    public void notificaConferma(UUID eventId, UUID ordineId) {
        if (!processedEventInserter.tryInsert(eventId)) return;
        log.info("Email simulata: ordine {} CONFERMATO", ordineId);
        salvaLog(ordineId, "CONFERMA", "Ordine " + ordineId + " confermato");
    }

    @Transactional
    public void notificaFallimento(UUID eventId, UUID ordineId, String motivo) {
        if (!processedEventInserter.tryInsert(eventId)) return;
        log.info("Email simulata: ordine {} FALLITO — {}", ordineId, motivo);
        salvaLog(ordineId, "FALLIMENTO", "Ordine " + ordineId + " fallito: " + motivo);
    }

    @Transactional
    public void notificaAnnullamento(UUID eventId, UUID ordineId) {
        if (!processedEventInserter.tryInsert(eventId)) return;
        log.info("Email simulata: ordine {} ANNULLATO", ordineId);
        salvaLog(ordineId, "ANNULLAMENTO", "Ordine " + ordineId + " annullato");
    }

    private void salvaLog(UUID ordineId, String tipo, String messaggio) {
        NotificaLog entry = new NotificaLog();
        entry.setId(UUID.randomUUID());
        entry.setOrdineId(ordineId);
        entry.setTipo(tipo);
        entry.setMessaggio(messaggio);
        entry.setCreatedAt(LocalDateTime.now());
        notificaLogRepository.save(entry);
    }
}
