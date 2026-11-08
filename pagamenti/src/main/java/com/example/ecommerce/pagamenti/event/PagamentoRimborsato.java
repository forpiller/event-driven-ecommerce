package com.example.ecommerce.pagamenti.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagamentoRimborsato {

    private UUID eventId;
    private UUID ordineId;
    private BigDecimal importo;
}
