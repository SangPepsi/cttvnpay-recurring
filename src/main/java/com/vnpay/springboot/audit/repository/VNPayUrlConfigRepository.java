package com.vnpay.springboot.audit.repository;

import com.vnpay.springboot.audit.entity.VNPayAuditFlow;
import com.vnpay.springboot.audit.entity.VNPayUrlConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VNPayUrlConfigRepository extends JpaRepository<VNPayUrlConfig, Long> {

    Optional<VNPayUrlConfig> findByFlow(VNPayAuditFlow flow);
}
