package com.vnpay.springboot.audit.repository;

import com.vnpay.springboot.audit.entity.VNPayAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface VNPayAuditLogRepository extends JpaRepository<VNPayAuditLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
    java.util.List<VNPayAuditLog> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
}

