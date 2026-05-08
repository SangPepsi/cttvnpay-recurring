package com.vnpay.springboot.Config;

import com.vnpay.springboot.audit.entity.VNPayAuditFlow;
import com.vnpay.springboot.audit.service.VNPayUrlConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Logs Return URL and IPN URL for this standalone demo.
 */
@Component
@Order(1)
public class VNPayUrlStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VNPayUrlStartupLogger.class);

    private final VNPayRecurringConfig recurringConfig;
    private final VNPayUrlConfigService urlConfigService;

    public VNPayUrlStartupLogger(VNPayRecurringConfig recurringConfig, VNPayUrlConfigService urlConfigService) {
        this.recurringConfig = recurringConfig;
        this.urlConfigService = urlConfigService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String returnUrl = recurringConfig.getReturnUrl();
        String ipnUrl = returnUrl.replace("/return", "") + "/ipn";
        urlConfigService.saveUrlConfig(VNPayAuditFlow.RECURRING, returnUrl, ipnUrl);
        log.info("=== VNPAY Recurring Demo Return URL & IPN URL ===");
        log.info("Return URL: {}", returnUrl);
        log.info("IPN URL:    {}", ipnUrl);
    }
}