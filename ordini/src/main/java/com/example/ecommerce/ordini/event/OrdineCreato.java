package com.example.ecommerce.ordini.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdineCreato {

    private UUID eventId;
    private UUID ordineId;
    private String prodottoId;
    private int quantita;
    private BigDecimal importoTotale;
}
