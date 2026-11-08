package com.example.ecommerce.notifiche.event;

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
public class PagamentoRimborsato {

    private UUID eventId;
    private UUID ordineId;
    private BigDecimal importo;
}
