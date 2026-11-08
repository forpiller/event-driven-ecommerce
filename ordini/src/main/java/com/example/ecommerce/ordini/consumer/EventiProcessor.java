package com.example.ecommerce.ordini.consumer;

import com.example.ecommerce.ordini.entity.OrdineStato;
import com.example.ecommerce.ordini.event.MerceRiservata;
import com.example.ecommerce.ordini.event.PagamentoEseguito;
import com.example.ecommerce.ordini.event.PagamentoFallito;
import com.example.ecommerce.ordini.event.PagamentoRimborsato;
import com.example.ecommerce.ordini.service.OrdineService;
import com.example.ecommerce.ordini.service.ProcessedEventInserter;
import org.springframework.transaction.annotation.Transactional;

// Classe plain, registrata come @Bean in ServiceConfig.
// Separata da EventiConsumer per evitare il proxy-bypass di Spring AOP:
// EventiConsumer riceve la stringa Kafka, deserializza, e delega qui passando per il proxy.
public class EventiProcessor {

    private final OrdineService ordineService;
    private final ProcessedEventInserter processedEventInserter;

    public EventiProcessor(OrdineService ordineService, ProcessedEventInserter processedEventInserter) {
        this.ordineService = ordineService;
        this.processedEventInserter = processedEventInserter;
    }

    @Transactional
    public void onPagamentoEseguito(PagamentoEseguito evento) {
        if (!processedEventInserter.tryInsert(evento.getEventId())) return;
        ordineService.aggiornaStato(evento.getOrdineId(), OrdineStato.PAGATO);
    }

    @Transactional
    public void onMerceRiservata(MerceRiservata evento) {
        if (!processedEventInserter.tryInsert(evento.getEventId())) return;
        ordineService.aggiornaStato(evento.getOrdineId(), OrdineStato.CONFERMATO);
    }

    @Transactional
    public void onPagamentoRimborsato(PagamentoRimborsato evento) {
        if (!processedEventInserter.tryInsert(evento.getEventId())) return;
        ordineService.aggiornaStato(evento.getOrdineId(), OrdineStato.CANCELLATO);
    }

    @Transactional
    public void onPagamentoFallito(PagamentoFallito evento) {
        if (!processedEventInserter.tryInsert(evento.getEventId())) return;
        ordineService.aggiornaStato(evento.getOrdineId(), OrdineStato.FALLITO);
    }
}
