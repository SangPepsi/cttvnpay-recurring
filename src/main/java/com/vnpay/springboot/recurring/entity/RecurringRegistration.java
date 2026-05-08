package com.vnpay.springboot.recurring.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.vnpay.springboot.recurring.entity.RecurringStatus;

@Entity
@Table(name = "recurring_registrations", indexes = {
        @Index(name = "idx_recurring_order_reference", columnList = "orderReference", unique = true)
})
public class RecurringRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String reqId;

    @Column(nullable = false, length = 12, unique = true)
    private String orderReference;

    @Column(nullable = false, length = 255)
    private String orderInfo;

    @Column(nullable = false, length = 100)
    private String orderType;

    @Column(nullable = false)
    private int recurringFrequencyNumber;

    @Column(nullable = false, length = 20)
    private String recurringFrequency;

    @Column(nullable = false)
    private int recurringNumber;

    @Column(nullable = false)
    private long recurringAmount;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currCode;

    @Column(nullable = false, length = 255)
    private String appUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringStatus status = RecurringStatus.PENDING;

    @Column(length = 20)
    private String rspCode;

    @Column(length = 255)
    private String rspMsg;

    @Column(length = 64)
    private String transactionId;

    @Column(length = 1000)
    private String dataKey;

    @Column(length = 100)
    private String recurringId;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public int getRecurringFrequencyNumber() {
        return recurringFrequencyNumber;
    }

    public void setRecurringFrequencyNumber(int recurringFrequencyNumber) {
        this.recurringFrequencyNumber = recurringFrequencyNumber;
    }

    public String getRecurringFrequency() {
        return recurringFrequency;
    }

    public void setRecurringFrequency(String recurringFrequency) {
        this.recurringFrequency = recurringFrequency;
    }

    public int getRecurringNumber() {
        return recurringNumber;
    }

    public void setRecurringNumber(int recurringNumber) {
        this.recurringNumber = recurringNumber;
    }

    public long getRecurringAmount() {
        return recurringAmount;
    }

    public void setRecurringAmount(long recurringAmount) {
        this.recurringAmount = recurringAmount;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getCurrCode() {
        return currCode;
    }

    public void setCurrCode(String currCode) {
        this.currCode = currCode;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(String appUserId) {
        this.appUserId = appUserId;
    }

    public RecurringStatus getStatus() {
        return status;
    }

    public void setStatus(RecurringStatus status) {
        this.status = status;
    }

    public String getRspCode() {
        return rspCode;
    }

    public void setRspCode(String rspCode) {
        this.rspCode = rspCode;
    }

    public String getRspMsg() {
        return rspMsg;
    }

    public void setRspMsg(String rspMsg) {
        this.rspMsg = rspMsg;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public String getRecurringId() {
        return recurringId;
    }

    public void setRecurringId(String recurringId) {
        this.recurringId = recurringId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
