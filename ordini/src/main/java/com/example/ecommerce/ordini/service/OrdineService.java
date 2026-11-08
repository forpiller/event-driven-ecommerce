package com.example.ecommerce.ordini.service;

import com.example.ecommerce.ordini.domain.CreaOrdineRequest;
import com.example.ecommerce.ordini.domain.OrdineDto;
import com.example.ecommerce.ordini.entity.Ordine;
import com.example.ecommerce.ordini.entity.OrdineStato;
import com.example.ecommerce.ordini.entity.OutboxEvent;
import com.example.ecommerce.ordini.event.OrdineCreato;
import com.example.ecommerce.ordini.repository.OrdineRepository;
import com.example.ecommerce.ordini.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrdineService {

    private static final Logger log = LoggerFactory.getLogger(OrdineService.class);

    private static final Map<OrdineStato, Set<OrdineStato>> TRANSIZIONI_VALIDE = Map.of(
            OrdineStato.CREATO, Set.of(OrdineStato.PAGATO, OrdineStato.FALLITO),
            OrdineStato.PAGATO, Set.of(OrdineStato.CONFERMATO, OrdineStato.CANCELLATO)
    );

    private final OrdineRepository ordineRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrdineService(OrdineRepository ordineRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.ordineRepository = ordineRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrdineDto creaOrdine(CreaOrdineRequest req) {
        Ordine ordine = new Ordine();
        ordine.setId(UUID.randomUUID());
        ordine.setProdottoId(req.getProdottoId());
        ordine.setQuantita(req.getQuantita());
        ordine.setImportoTotale(req.getImportoTotale());
        ordine.setStato(OrdineStato.CREATO);
        ordine.setCreatedAt(LocalDateTime.now());
        ordineRepository.save(ordine);

        OrdineCreato evento = new OrdineCreato(
                UUID.randomUUID(),
                ordine.getId(),
                ordine.getProdottoId(),
                ordine.getQuantita(),
                ordine.getImportoTotale()
        );

        String payload;
        try {
            payload = objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Impossibile serializzare OrdineCreato", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType("Ordine");
        outboxEvent.setAggregateId(ordine.getId().toString());
        outboxEvent.setEventType("OrdineCreato");
        outboxEvent.setPayload(payload);
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvent);

        return mapToDto(ordine);
    }

    @Transactional(readOnly = true)
    public OrdineDto trovaPerId(UUID id) {
        Ordine ordine = ordineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato: " + id));
        return mapToDto(ordine);
    }

    @Transactional
    public void aggiornaStato(UUID ordineId, OrdineStato nuovoStato) {
        Ordine ordine = ordineRepository.findById(ordineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordine non trovato: " + ordineId));

        Set<OrdineStato> statiValidi = TRANSIZIONI_VALIDE.getOrDefault(ordine.getStato(), Set.of());
        if (!statiValidi.contains(nuovoStato)) {
            log.warn("Transizione non valida da {} a {} per ordine {}", ordine.getStato(), nuovoStato, ordineId);
            return;
        }

        ordine.setStato(nuovoStato);
        ordineRepository.save(ordine);
    }

    private OrdineDto mapToDto(Ordine ordine) {
        OrdineDto dto = new OrdineDto();
        dto.setId(ordine.getId());
        dto.setProdottoId(ordine.getProdottoId());
        dto.setQuantita(ordine.getQuantita());
        dto.setImportoTotale(ordine.getImportoTotale());
        dto.setStato(ordine.getStato());
        return dto;
    }
}
