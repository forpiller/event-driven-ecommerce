package com.example.ecommerce.pagamenti.service;

public class GatewayBancaException extends RuntimeException {

    public GatewayBancaException(String message) {
        super(message);
    }

    public GatewayBancaException(String message, Throwable cause) {
        super(message, cause);
    }
}
