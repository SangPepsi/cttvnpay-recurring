package com.vnpay.springboot.audit.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Lưu cấu hình Return URL và IPN URL theo từng flow.
 * Được ghi vào DB khi ứng dụng khởi động.
 */
@Entity
@Table(name = "vnpay_url_config", indexes = {
        @Index(name = "idx_vnpay_url_config_flow", columnList = "flow", unique = true)
})
public class VNPayUrlConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow", nullable = false, unique = true, length = 20)
    private VNPayAuditFlow flow;

    @Column(name = "return_url", nullable = false, length = 2000)
    private String returnUrl;

    @Column(name = "ipn_url", nullable = false, length = 2000)
    private String ipnUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public VNPayAuditFlow getFlow() {
        return flow;
    }

    public void setFlow(VNPayAuditFlow flow) {
        this.flow = flow;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getIpnUrl() {
        return ipnUrl;
    }

    public void setIpnUrl(String ipnUrl) {
        this.ipnUrl = ipnUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
