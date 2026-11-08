package com.example.ecommerce.pagamenti.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerceNonDisponibile {

    private UUID eventId;
    private UUID ordineId;
    private String prodottoId;
    private int quantitaRichiesta;
}
