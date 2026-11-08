package com.example.ecommerce.magazzino.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoEseguito {

    private UUID eventId;
    private UUID ordineId;
    private BigDecimal importo;
    private String prodottoId;
    private int quantita;
}
