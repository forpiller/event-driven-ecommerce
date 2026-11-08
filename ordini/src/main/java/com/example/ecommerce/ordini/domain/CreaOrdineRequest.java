package com.example.ecommerce.ordini.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreaOrdineRequest {

    @NotBlank
    private String prodottoId;

    @Min(1)
    private int quantita;

    @NotNull
    @Positive
    private BigDecimal importoTotale;
}
