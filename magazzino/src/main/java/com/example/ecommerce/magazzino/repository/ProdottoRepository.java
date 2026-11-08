package com.example.ecommerce.magazzino.repository;

import com.example.ecommerce.magazzino.entity.Prodotto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProdottoRepository extends JpaRepository<Prodotto, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Prodotto> findWithLockById(String id);
}
