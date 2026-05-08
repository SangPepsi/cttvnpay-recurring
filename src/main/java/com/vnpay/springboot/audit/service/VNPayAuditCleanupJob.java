package com.vnpay.springboot.audit.service;

import com.vnpay.springboot.audit.repository.VNPayAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class VNPayAuditCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(VNPayAuditCleanupJob.class);

    private final VNPayAuditLogRepository repo;

    @Value("${vnpay.audit.retention-days:30}")
    private int retentionDays;

    public VNPayAuditCleanupJob(VNPayAuditLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Cleanup audit logs to keep DB lightweight.
     * Default runs daily at 03:00 server time.
     */
    @Scheduled(cron = "${vnpay.audit.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldAuditLogs() {
        if (retentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long deleted = repo.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Audit cleanup: deleted {} rows older than {} days (cutoff={})", deleted, retentionDays, cutoff);
        }
    }
}

