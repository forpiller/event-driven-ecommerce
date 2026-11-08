package com.example.ecommerce.ordini.repository;

import com.example.ecommerce.ordini.entity.Ordine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdineRepository extends JpaRepository<Ordine, UUID> {
}
