package com.example.ecommerce.notifiche.repository;

import com.example.ecommerce.notifiche.entity.NotificaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificaLogRepository extends JpaRepository<NotificaLog, UUID> {
}
