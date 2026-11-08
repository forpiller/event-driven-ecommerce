package com.example.ecommerce.notifiche.config;

import com.example.ecommerce.notifiche.repository.NotificaLogRepository;
import com.example.ecommerce.notifiche.service.NotificheService;
import com.example.ecommerce.notifiche.service.ProcessedEventInserter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class ServiceConfig {

    @Bean
    public ProcessedEventInserter processedEventInserter(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventInserter(jdbcTemplate);
    }

    @Bean
    public NotificheService notificheService(NotificaLogRepository notificaLogRepository,
                                              ProcessedEventInserter processedEventInserter) {
        return new NotificheService(notificaLogRepository, processedEventInserter);
    }
}
