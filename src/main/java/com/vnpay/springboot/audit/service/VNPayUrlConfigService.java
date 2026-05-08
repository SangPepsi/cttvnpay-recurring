package com.vnpay.springboot.audit.service;

import com.vnpay.springboot.audit.entity.VNPayAuditFlow;
import com.vnpay.springboot.audit.entity.VNPayUrlConfig;
import com.vnpay.springboot.audit.repository.VNPayUrlConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VNPayUrlConfigService {

    private static final Logger log = LoggerFactory.getLogger(VNPayUrlConfigService.class);

    private final VNPayUrlConfigRepository repo;

    public VNPayUrlConfigService(VNPayUrlConfigRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void saveUrlConfig(VNPayAuditFlow flow, String returnUrl, String ipnUrl) {
        if (returnUrl == null || ipnUrl == null) {
            log.warn("Skipping URL config for flow {}: returnUrl or ipnUrl null", flow);
            return;
        }
        VNPayUrlConfig config = repo.findByFlow(flow)
                .orElse(new VNPayUrlConfig());
        config.setFlow(flow);
        config.setReturnUrl(returnUrl);
        config.setIpnUrl(ipnUrl);
        repo.save(config);
        log.debug("Saved URL config for flow {}: return={}, ipn={}", flow, returnUrl, ipnUrl);
    }
}
