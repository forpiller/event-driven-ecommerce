package com.example.ecommerce.magazzino.web.rest;

import com.example.ecommerce.magazzino.domain.SetStockRequest;
import com.example.ecommerce.magazzino.domain.StockDto;
import com.example.ecommerce.magazzino.service.MagazzinoService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Profile("local")
public class StockRest {

    private final MagazzinoService magazzinoService;

    public StockRest(MagazzinoService magazzinoService) {
        this.magazzinoService = magazzinoService;
    }

    @GetMapping("/stock/{prodottoId}")
    public StockDto getStock(@PathVariable String prodottoId) {
        return magazzinoService.getStock(prodottoId);
    }

    @PutMapping("/stock")
    public void setStock(@RequestBody SetStockRequest req) {
        magazzinoService.setStock(req.getProdottoId(), req.getQuantita());
    }
}
