package com.example.ecommerce.magazzino.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerceNonDisponibile {

    private UUID eventId;
    private UUID ordineId;
    private String prodottoId;
    private int quantitaRichiesta;
}
