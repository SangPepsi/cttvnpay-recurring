package com.vnpay.springboot.recurring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RecurringInitRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_\\-]{1,12}$")
    private String orderReference;

    @Pattern(regexp = "^(recurring|pay_n_recurring)?$")
    private String command = "pay_n_recurring";

    @NotBlank
    @Size(max = 255)
    private String orderInfo;

    @NotBlank
    @Size(max = 100)
    private String orderType;

    @Min(1)
    private int recurringFrequencyNumber;

    @NotBlank
    @Pattern(regexp = "^(day|week|month|quarter|year)$")
    private String recurringFrequency;

    @Min(0)
    private int recurringNumber;

    @Min(1)
    private long recurringAmount;

    @Min(0)
    private long amount;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_\\-]{1,255}$")
    private String appUserId;

    @NotBlank
    @Pattern(regexp = "^\\d{8}$")
    private String recurringDate;

    @NotBlank
    @Pattern(regexp = "^\\d{8}$")
    private String recurringStartDate;

    @NotBlank
    @Pattern(regexp = "^\\d{8}$")
    private String recurringEndDate;

    @Size(max = 2000)
    private String addData;

    @NotBlank
    @Pattern(regexp = "^(vn|en)$")
    private String locale;

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
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

    public String getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(String appUserId) {
        this.appUserId = appUserId;
    }

    public String getRecurringDate() {
        return recurringDate;
    }

    public void setRecurringDate(String recurringDate) {
        this.recurringDate = recurringDate;
    }

    public String getRecurringStartDate() {
        return recurringStartDate;
    }

    public void setRecurringStartDate(String recurringStartDate) {
        this.recurringStartDate = recurringStartDate;
    }

    public String getRecurringEndDate() {
        return recurringEndDate;
    }

    public void setRecurringEndDate(String recurringEndDate) {
        this.recurringEndDate = recurringEndDate;
    }

    public String getAddData() {
        return addData;
    }

    public void setAddData(String addData) {
        this.addData = addData;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
