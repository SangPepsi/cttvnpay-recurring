package com.vnpay.springboot.recurring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RecurringPayRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,100}$")
    private String orderReference;

    @NotBlank
    @Size(max = 255)
    private String orderInfo;

    @NotBlank
    @Pattern(regexp = "^\\d{1,18}$")
    private String recurringId;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,64}$")
    private String tokenId;

    @Min(1)
    private long recurringAmount;

    @NotBlank
    @Pattern(regexp = "^\\d{8}$")
    private String recurringDate;

    @Size(max = 2000)
    private String addData;

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

    public String getRecurringId() {
        return recurringId;
    }

    public void setRecurringId(String recurringId) {
        this.recurringId = recurringId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public long getRecurringAmount() {
        return recurringAmount;
    }

    public void setRecurringAmount(long recurringAmount) {
        this.recurringAmount = recurringAmount;
    }

    public String getRecurringDate() {
        return recurringDate;
    }

    public void setRecurringDate(String recurringDate) {
        this.recurringDate = recurringDate;
    }

    public String getAddData() {
        return addData;
    }

    public void setAddData(String addData) {
        this.addData = addData;
    }
}
