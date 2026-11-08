package com.example.ecommerce.pagamenti.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoFallito {

    private UUID eventId;
    private UUID ordineId;
    private String motivo;
}
