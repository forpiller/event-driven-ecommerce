package com.example.ecommerce.ordini.web.rest;

import com.example.ecommerce.ordini.domain.CreaOrdineRequest;
import com.example.ecommerce.ordini.domain.OrdineDto;
import com.example.ecommerce.ordini.service.OrdineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ordini")
public class OrdineRest {

    private final OrdineService ordineService;

    public OrdineRest(OrdineService ordineService) {
        this.ordineService = ordineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdineDto crea(@Valid @RequestBody CreaOrdineRequest req) {
        return ordineService.creaOrdine(req);
    }

    @GetMapping("/{id}")
    public OrdineDto trova(@PathVariable UUID id) {
        return ordineService.trovaPerId(id);
    }
}
