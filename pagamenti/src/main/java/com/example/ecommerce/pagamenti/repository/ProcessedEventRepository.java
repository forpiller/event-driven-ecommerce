package com.example.ecommerce.pagamenti.repository;

import com.example.ecommerce.pagamenti.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
