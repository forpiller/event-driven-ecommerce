package com.example.ecommerce.pagamenti.web.rest;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("local")
@RequestMapping("/internal/gateway")
public class GatewayBancaSimulator {

    @PostMapping
    public void gateway(@RequestParam(defaultValue = "false") boolean fail) {
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gateway simulato: errore");
        }
    }
}
