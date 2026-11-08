package com.example.ecommerce.magazzino.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetStockRequest {

    private String prodottoId;
    private int quantita;
}
