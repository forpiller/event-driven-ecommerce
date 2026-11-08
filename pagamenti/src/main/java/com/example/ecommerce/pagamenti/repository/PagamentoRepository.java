package com.example.ecommerce.pagamenti.repository;

import com.example.ecommerce.pagamenti.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    List<Pagamento> findByOrdineId(UUID ordineId);

    java.util.Optional<Pagamento> findFirstByOrdineId(UUID ordineId);
}
