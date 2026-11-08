package com.example.ecommerce.ordini.domain;

import com.example.ecommerce.ordini.entity.OrdineStato;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrdineDto {

    private UUID id;
    private String prodottoId;
    private int quantita;
    private BigDecimal importoTotale;
    private OrdineStato stato;
}
