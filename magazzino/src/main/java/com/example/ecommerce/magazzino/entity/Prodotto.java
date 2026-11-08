package com.example.ecommerce.magazzino.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prodotti")
@Getter
@Setter
public class Prodotto {

    @Id
    private String id;

    private String nome;

    private int quantita;
}
