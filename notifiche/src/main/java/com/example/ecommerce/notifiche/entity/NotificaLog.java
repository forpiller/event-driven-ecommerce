package com.example.ecommerce.notifiche.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifica_log", indexes = {
        @Index(name = "idx_notifica_log_ordine_id", columnList = "ordine_id")
})
@Getter
@Setter
public class NotificaLog {

    @Id
    private UUID id;

    @Column(name = "ordine_id", nullable = false)
    private UUID ordineId;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false)
    private String messaggio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
