package com.example.ecommerce.pagamenti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamenti", indexes = {
    @Index(name = "idx_pagamenti_ordine_id", columnList = "ordine_id")
})
@Getter
@Setter
public class Pagamento {

    @Id
    private UUID id;

    @Column(name = "ordine_id", nullable = false)
    private UUID ordineId;

    @Column(name = "importo", nullable = false, precision = 10, scale = 2)
    private BigDecimal importo;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 30)
    private PagamentoStato stato;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
