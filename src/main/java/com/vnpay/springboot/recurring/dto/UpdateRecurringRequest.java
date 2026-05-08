package com.vnpay.springboot.recurring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateRecurringRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{1,18}$")
    private String recurringId;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,64}$")
    private String tokenId;

    @Min(1)
    private int recurringNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{8}$")
    private String recurringEndDate;

    @Min(1)
    private long amount;

    @Size(max = 2000)
    private String addData;

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

    public int getRecurringNumber() {
        return recurringNumber;
    }

    public void setRecurringNumber(int recurringNumber) {
        this.recurringNumber = recurringNumber;
    }

    public String getRecurringEndDate() {
        return recurringEndDate;
    }

    public void setRecurringEndDate(String recurringEndDate) {
        this.recurringEndDate = recurringEndDate;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getAddData() {
        return addData;
    }

    public void setAddData(String addData) {
        this.addData = addData;
    }
}
