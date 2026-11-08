package com.example.ecommerce.ordini.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoFallito {

    private UUID eventId;
    private UUID ordineId;
    private String motivo;
}
