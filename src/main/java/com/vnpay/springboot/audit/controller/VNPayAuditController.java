package com.vnpay.springboot.audit.controller;

import com.vnpay.springboot.audit.entity.VNPayUrlConfig;
import com.vnpay.springboot.audit.repository.VNPayUrlConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API xem cấu hình Return URL, IPN URL đã lưu trong DB.
 */
@RestController
@RequestMapping("/vnpay/audit")
public class VNPayAuditController {

    private final VNPayUrlConfigRepository urlConfigRepo;

    public VNPayAuditController(VNPayUrlConfigRepository urlConfigRepo) {
        this.urlConfigRepo = urlConfigRepo;
    }

    @GetMapping("/url-config")
    public ResponseEntity<Map<String, Object>> getUrlConfig() {
        List<VNPayUrlConfig> configs = urlConfigRepo.findAll();
        List<Map<String, Object>> items = configs.stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("flow", c.getFlow().name());
                    m.put("returnUrl", c.getReturnUrl());
                    m.put("ipnUrl", c.getIpnUrl());
                    m.put("updatedAt", c.getUpdatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("urlConfigs", items);
        return ResponseEntity.ok(body);
    }
}
