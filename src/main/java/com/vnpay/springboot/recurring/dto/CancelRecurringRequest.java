package com.vnpay.springboot.recurring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CancelRecurringRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{1,18}$")
    private String recurringId;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,64}$")
    private String tokenId;

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

    public String getAddData() {
        return addData;
    }

    public void setAddData(String addData) {
        this.addData = addData;
    }
}
