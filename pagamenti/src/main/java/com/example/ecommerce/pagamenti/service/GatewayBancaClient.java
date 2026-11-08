package com.example.ecommerce.pagamenti.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

public class GatewayBancaClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayBancaClient.class);

    private final RestTemplate restTemplate;
    private final String gatewayUrl;

    public GatewayBancaClient(RestTemplate restTemplate, String gatewayUrl) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
    }

    @CircuitBreaker(name = "gateway-banca", fallbackMethod = "fallback")
    @Retry(name = "gateway-banca")
    public void addebita(BigDecimal importo) {
        restTemplate.postForEntity(gatewayUrl + "/internal/gateway", null, Void.class);
    }

    public void fallback(BigDecimal importo, Throwable t) {
        log.warn("Fallback gateway bancario per importo {}: {}", importo, t.getMessage());
        throw new GatewayBancaException("Gateway non disponibile", t);
    }
}
