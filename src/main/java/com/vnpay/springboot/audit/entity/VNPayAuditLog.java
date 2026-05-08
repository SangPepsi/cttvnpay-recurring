package com.vnpay.springboot.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "vnpay_audit_logs", indexes = {
        @Index(name = "idx_vnpay_audit_flow_created", columnList = "flow, created_at"),
        @Index(name = "idx_vnpay_audit_corr_id", columnList = "correlation_id"),
        @Index(name = "idx_vnpay_audit_direction", columnList = "direction")
})
public class VNPayAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow", nullable = false, length = 20)
    private VNPayAuditFlow flow;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private VNPayAuditDirection direction;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "method", length = 10)
    private String method;

    /** TEXT inline — tránh @Lob + PostgreSQL OID (lỗi "Large Objects may not be used in auto-commit mode") */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "url", columnDefinition = "text")
    private String url;

    @Column(name = "remote_ip", length = 45)
    private String remoteIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "response_status")
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "request_data", columnDefinition = "text")
    private String requestData;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "response_data", columnDefinition = "text")
    private String responseData;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
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

    public VNPayAuditDirection getDirection() {
        return direction;
    }

    public void setDirection(VNPayAuditDirection direction) {
        this.direction = direction;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

