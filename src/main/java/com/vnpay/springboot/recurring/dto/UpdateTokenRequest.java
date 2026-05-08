package com.vnpay.springboot.recurring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateTokenRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,100}$")
    private String orderReference;

    @NotBlank
    @Size(max = 255)
    private String orderInfo;

    @NotBlank
    @Pattern(regexp = "^\\d{1,18}$")
    private String recurringId;

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

    public String getAddData() {
        return addData;
    }

    public void setAddData(String addData) {
        this.addData = addData;
    }
}
