package com.vnpay.springboot.recurring.service;

import com.vnpay.springboot.Config.VNPayRecurringConfig;
import com.vnpay.springboot.Util.VNPayUtils;
import com.vnpay.springboot.audit.service.VNPayAuditLogService;
import com.vnpay.springboot.recurring.entity.RecurringRegistration;
import com.vnpay.springboot.recurring.entity.RecurringStatus;
import com.vnpay.springboot.recurring.repository.RecurringRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VNPayRecurringSitTest {

    private static final String SECRET_KEY = "RECURRING_SECRET_TEST_KEY";

    private VNPayRecurringConfig config;
    private RecurringRegistrationRepository repository;
    private VNPayRecurringService service;

    @BeforeEach
    void setUp() {
        config = new VNPayRecurringConfig();
        config.setSecretKey(SECRET_KEY);
        config.setClientId("clientId");
        config.setClientSecret("clientSecret");
        config.setUsername("username");
        config.setPassword("password");
        config.setTmnCode("TMN_TEST");
        config.setApiUrl("https://sandbox.vnpayment.vn/isp-svc");
        config.setPayUrl("https://sandbox.vnpayment.vn/isp-svc/recurring-payment/pay");
        config.setReturnUrl("http://localhost:9999/vnpay/recurring/return");
        config.setCancelUrl("http://localhost:9999/vnpay/recurring/return");

        repository = mock(RecurringRegistrationRepository.class);
        WebClient webClient = mock(WebClient.class);
        VNPayAuditLogService auditLogService = mock(VNPayAuditLogService.class);
        service = new VNPayRecurringService(config, webClient, repository, auditLogService);
    }

    @Test
    void sitCase15_ipnSuccess_shouldConfirmAndMarkOrderSuccess() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER15", 1000000);
        when(repository.findByOrderReference("ORDER15")).thenReturn(Optional.of(registration));
        when(repository.save(any(RecurringRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = buildSignedIpnParams("ORDER15", "00", "00", 1000000);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("00");
        assertThat(registration.getStatus()).isEqualTo(RecurringStatus.SUCCESS);
    }

    @Test
    void sitCase16_ipnFailedAuth_shouldStillReturn00AndMarkFailed() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER16", 1000000);
        when(repository.findByOrderReference("ORDER16")).thenReturn(Optional.of(registration));
        when(repository.save(any(RecurringRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = buildSignedIpnParams("ORDER16", "24", "24", 1000000);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("00");
        assertThat(registration.getStatus()).isEqualTo(RecurringStatus.FAILED);
    }

    @Test
    void sitCase17_ipnOrderNotFound_shouldReturn01() throws Exception {
        when(repository.findByOrderReference("NOT_FOUND")).thenReturn(Optional.empty());

        Map<String, String> params = buildSignedIpnParams("NOT_FOUND", "00", "00", 1000000);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("01");
        assertThat(response.get("Message")).containsIgnoringCase("not");
    }

    @Test
    void sitCase18_ipnAlreadyConfirmed_shouldReturn02() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER18", 1000000);
        registration.setStatus(RecurringStatus.SUCCESS);
        when(repository.findByOrderReference("ORDER18")).thenReturn(Optional.of(registration));

        Map<String, String> params = buildSignedIpnParams("ORDER18", "00", "00", 1000000);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("02");
    }

    @Test
    void sitCase19_ipnInvalidAmount_shouldReturn04() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER19", 10000);
        when(repository.findByOrderReference("ORDER19")).thenReturn(Optional.of(registration));

        Map<String, String> params = buildSignedIpnParams("ORDER19", "00", "00", 12345);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("04");
    }

    @Test
    void sitCase20_ipnInvalidSignature_shouldReturn97() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_txn_ref", "ORDER20");
        params.put("vnp_response_code", "00");
        params.put("vnp_transaction_status", "00");
        params.put("vnp_amount", "1000000");
        params.put("vnp_secure_hash", "invalid-signature");

        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("97");
    }

    @Test
    void sitCase21_ipnOtherException_shouldReturn99() throws Exception {
        when(repository.findByOrderReference("ORDER21")).thenThrow(new RuntimeException("DB offline"));

        Map<String, String> params = buildSignedIpnParams("ORDER21", "00", "00", 1000000);
        Map<String, String> response = service.processRecurringIpn(params);

        assertThat(response.get("RspCode")).isEqualTo("99");
    }

    @Test
    void sitCase5_returnSuccess_shouldMarkOrderSuccess() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER5", 10000);
        when(repository.findByOrderReference("ORDER5")).thenReturn(Optional.of(registration));
        when(repository.save(any(RecurringRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = buildSignedReturnParams("ORDER5", "00", "00");
        VNPayRecurringService.RecurringReturnResult result = service.processRecurringReturn(params);

        assertThat(result.status()).isEqualTo(1);
        assertThat(registration.getStatus()).isEqualTo(RecurringStatus.SUCCESS);
    }

    @Test
    void sitCase6_returnFailed_shouldMarkOrderFailed() throws Exception {
        RecurringRegistration registration = pendingRegistration("ORDER6", 10000);
        when(repository.findByOrderReference("ORDER6")).thenReturn(Optional.of(registration));
        when(repository.save(any(RecurringRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = buildSignedReturnParams("ORDER6", "24", "24");
        VNPayRecurringService.RecurringReturnResult result = service.processRecurringReturn(params);

        assertThat(result.status()).isEqualTo(0);
        assertThat(registration.getStatus()).isEqualTo(RecurringStatus.FAILED);
    }

    private RecurringRegistration pendingRegistration(String orderReference, long amount) {
        RecurringRegistration registration = new RecurringRegistration();
        registration.setOrderReference(orderReference);
        registration.setAmount(amount);
        registration.setStatus(RecurringStatus.PENDING);
        return registration;
    }

    private Map<String, String> buildSignedIpnParams(
            String orderReference,
            String responseCode,
            String transactionStatus,
            long amountMinor
    ) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_txn_ref", orderReference);
        params.put("vnp_response_code", responseCode);
        params.put("vnp_transaction_status", transactionStatus);
        params.put("vnp_amount", String.valueOf(amountMinor));
        params.put("vnp_transaction_no", "123456");
        params.put("vnp_tmn_code", "TMN_TEST");
        params.put("vnp_command", "pay_n_recurring");
        params.put("vnp_recurring_id", "RECUR-" + orderReference);
        params.put("vnp_app_user_id", "user-" + orderReference);
        params.put("vnp_token", "token-" + orderReference);
        params.put("vnp_pay_date", "20260409120000");
        params.put("vnp_message", "SIT");
        params.put("vnp_version", "2.1.0");
        params.put("vnp_locale", "vn");

        params.put("vnp_secure_hash", buildHash(params));
        return new HashMap<>(params);
    }

    private Map<String, String> buildSignedReturnParams(
            String orderReference,
            String responseCode,
            String transactionStatus
    ) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_txn_ref", orderReference);
        params.put("vnp_response_code", responseCode);
        params.put("vnp_transaction_status", transactionStatus);
        params.put("vnp_amount", "1000000");
        params.put("vnp_transaction_no", "123456");
        params.put("vnp_tmn_code", "TMN_TEST");
        params.put("vnp_command", "pay_n_recurring");
        params.put("vnp_pay_date", "20260409120000");
        params.put("vnp_version", "2.1.0");
        params.put("vnp_locale", "vn");
        params.put("vnp_recurring_id", "RECUR-" + orderReference);

        params.put("vnp_secure_hash", buildHash(params));
        return new HashMap<>(params);
    }

    private String buildHash(Map<String, String> params) throws Exception {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("vnp_secure_hash".equalsIgnoreCase(entry.getKey())
                    || "vnp_secure_hash_type".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String encoded = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString());
            hashData.append(entry.getKey()).append("=").append(encoded).append("&");
        }
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        return VNPayUtils.hmacSHA512(SECRET_KEY, hashData.toString());
    }
}
