package com.example.ecommerce.ordini.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordini")
@Getter
@Setter
public class Ordine {

    @Id
    private UUID id;

    @Column(name = "prodotto_id", nullable = false, length = 100)
    private String prodottoId;

    @Column(name = "quantita", nullable = false)
    private int quantita;

    @Column(name = "importo_totale", nullable = false, precision = 10, scale = 2)
    private BigDecimal importoTotale;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 30)
    private OrdineStato stato;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
