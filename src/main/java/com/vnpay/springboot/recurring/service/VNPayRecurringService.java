package com.vnpay.springboot.recurring.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vnpay.springboot.Config.VNPayRecurringConfig;
import com.vnpay.springboot.audit.entity.VNPayAuditFlow;
import com.vnpay.springboot.audit.service.VNPayAuditLogService;
import com.vnpay.springboot.recurring.dto.CancelRecurringRequest;
import com.vnpay.springboot.recurring.dto.RecurringActionResult;
import com.vnpay.springboot.recurring.dto.RecurringInitRequest;
import com.vnpay.springboot.recurring.dto.RecurringInitResult;
import com.vnpay.springboot.recurring.dto.RecurringPayRequest;
import com.vnpay.springboot.recurring.dto.UpdateRecurringRequest;
import com.vnpay.springboot.recurring.dto.UpdateTokenRequest;
import com.vnpay.springboot.recurring.entity.RecurringRegistration;
import com.vnpay.springboot.recurring.entity.RecurringStatus;
import com.vnpay.springboot.recurring.repository.RecurringRegistrationRepository;
import com.vnpay.springboot.Util.VNPayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayRecurringService {

    private static final Logger log = LoggerFactory.getLogger(VNPayRecurringService.class);
    private static final String RECURRING_VERSION = "2.1.0";
    private static final String DEFAULT_RECURRING_COMMAND = "pay_n_recurring";
    private static final String RECURRING_PAY_COMMAND = "recurring_pay";
    private static final String UPDATE_TOKEN_COMMAND = "update_token";
    private static final String UPDATE_RECURRING_COMMAND = "update_recurring";
    private static final String CANCEL_RECURRING_COMMAND = "cancel_recurring";

    private final VNPayRecurringConfig recurringConfig;
    private final WebClient recurringWebClient;
    private final RecurringRegistrationRepository recurringRepository;
    private final VNPayAuditLogService auditLogService;

    public VNPayRecurringService(
            VNPayRecurringConfig recurringConfig,
            @Qualifier("vnpayRecurringWebClient") WebClient recurringWebClient,
            RecurringRegistrationRepository recurringRepository,
            VNPayAuditLogService auditLogService) {
        this.recurringConfig = recurringConfig;
        this.recurringWebClient = recurringWebClient;
        this.recurringRepository = recurringRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RecurringInitResult initRecurring(RecurringInitRequest request, String clientIp, String userAgent) {
        log.info("Recurring init start: orderReference={}, clientIp={}, userAgent={}",
                request.getOrderReference(), clientIp, userAgent);
        if (recurringRepository.existsByOrderReference(request.getOrderReference())) {
            throw new IllegalArgumentException("Mã tham chiếu đơn hàng đã tồn tại.");
        }

        String reqId = String.valueOf(System.currentTimeMillis());
        String mcDate = getNowGmt7("yyyyMMddHHmmss");
        // Định dạng amount/recurringAmount theo đặc tả VNPAY (x100), giống form vnpay_recurring_init.html
        long computedAmount = request.getRecurringNumber() == 0
                ? request.getRecurringAmount()
                : request.getRecurringAmount() * request.getRecurringNumber();
        long totalAmount = request.getAmount() > 0 ? request.getAmount() : computedAmount;

        RecurringRegistration reg = new RecurringRegistration();
        reg.setReqId(reqId);
        reg.setOrderReference(request.getOrderReference());
        reg.setOrderInfo(request.getOrderInfo());
        reg.setOrderType(request.getOrderType());
        reg.setRecurringFrequencyNumber(request.getRecurringFrequencyNumber());
        reg.setRecurringFrequency(request.getRecurringFrequency());
        reg.setRecurringNumber(request.getRecurringNumber());
        reg.setRecurringAmount(request.getRecurringAmount());
        reg.setAmount(totalAmount);
        reg.setCurrCode("VND");
        reg.setAppUserId(request.getAppUserId());
        reg.setIpAddress(clientIp);
        reg.setUserAgent(userAgent);
        reg.setStatus(RecurringStatus.PENDING);
        recurringRepository.save(reg);

        log.info("Recurring init auth: apiUrl={}, clientId={}, username={}, tmnCode={}",
                recurringConfig.getApiUrl(),
                mask(recurringConfig.getClientId()),
                recurringConfig.getUsername(),
                recurringConfig.getTmnCode());
        String token = authenticate();

        log.info("Recurring init payload: orderReference={}, orderType={}, recurringFrequency={} {}, recurringNumber={}, recurringAmount={}, amount={}",
                request.getOrderReference(),
                request.getOrderType(),
                request.getRecurringFrequencyNumber(),
                request.getRecurringFrequency(),
                request.getRecurringNumber(),
                request.getRecurringAmount(),
                totalAmount);

        JsonObject payload = new JsonObject();
        payload.addProperty("reqId", reqId);
        String command = (request.getCommand() != null && !request.getCommand().isBlank())
                ? request.getCommand() : DEFAULT_RECURRING_COMMAND;
        payload.addProperty("command", command);
        payload.addProperty("tmnCode", recurringConfig.getTmnCode());
        payload.addProperty("version", RECURRING_VERSION);
        payload.addProperty("locale", request.getLocale());
        payload.addProperty("addData", defaultString(request.getAddData()));
        payload.addProperty("ipAddr", clientIp);
        payload.addProperty("userAgent", userAgent);

        JsonObject order = new JsonObject();
        order.addProperty("orderReference", request.getOrderReference());
        order.addProperty("orderInfo", request.getOrderInfo());
        order.addProperty("orderType", request.getOrderType());
        payload.add("order", order);

        JsonObject app = new JsonObject();
        app.addProperty("userId", request.getAppUserId());
        payload.add("app", app);

        JsonObject transaction = new JsonObject();
        transaction.addProperty("recurringFrequencyNumber", request.getRecurringFrequencyNumber());
        transaction.addProperty("recurringFrequency", request.getRecurringFrequency());
        transaction.addProperty("recurringNumber", request.getRecurringNumber());
        transaction.addProperty("recurringAmount", request.getRecurringAmount());
        transaction.addProperty("amount", totalAmount);
        transaction.addProperty("currCode", "VND");
        transaction.addProperty("returnUrl", recurringConfig.getReturnUrl());
        transaction.addProperty("cancelUrl", recurringConfig.getCancelUrl());
        transaction.addProperty("mcDate", mcDate);
        transaction.addProperty("recurringDate", request.getRecurringDate());
        transaction.addProperty("recurringStartDate", request.getRecurringStartDate());
        transaction.addProperty("recurringEndDate", request.getRecurringEndDate());
        payload.add("transaction", transaction);

        String hashData = buildInitHashData(
                reqId,
                command,
                request.getOrderReference(),
                request.getOrderInfo(),
                request.getOrderType(),
                recurringConfig.getTmnCode(),
                request.getRecurringAmount(),
                request.getRecurringFrequencyNumber(),
                request.getRecurringFrequency(),
                request.getRecurringNumber(),
                request.getRecurringDate(),
                request.getRecurringStartDate(),
                request.getRecurringEndDate(),
                totalAmount,
                "VND",
                defaultString(request.getAddData()),
                request.getAppUserId(),
                "",
                "",
                clientIp,
                userAgent,
                recurringConfig.getReturnUrl(),
                recurringConfig.getCancelUrl(),
                RECURRING_VERSION,
                request.getLocale(),
                mcDate
        );

        String secureHash = VNPayUtils.hmacSHA512(recurringConfig.getSecretKey(), hashData);
        payload.addProperty("secureHash", secureHash);

        log.info("Recurring init call: endpoint=/recurring-payment/execute");
        long startMs = System.currentTimeMillis();
        String url = recurringConfig.getApiUrl() + "/recurring-payment/execute";
        String responseJson = recurringWebClient.post()
                .uri("/recurring-payment/execute")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .bodyValue(payload.toString())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            if (response.statusCode().isError()) {
                                log.error("VNPAY recurring execute error: status={}, body={}", response.statusCode(), body);
                                auditLogService.logOutbound(
                                        VNPayAuditFlow.RECURRING,
                                        "RECURRING_EXECUTE",
                                        request.getOrderReference(),
                                        url,
                                        "POST",
                                        payload.toString(),
                                        response.statusCode().value(),
                                        body,
                                        System.currentTimeMillis() - startMs,
                                        "HTTP " + response.statusCode().value()
                                );
                                throw new IllegalStateException("VNPAY recurring execute failed: status="
                                        + response.statusCode().value());
                            }
                            auditLogService.logOutbound(
                                    VNPayAuditFlow.RECURRING,
                                    "RECURRING_EXECUTE",
                                    request.getOrderReference(),
                                    url,
                                    "POST",
                                    payload.toString(),
                                    response.statusCode().value(),
                                    body,
                                    System.currentTimeMillis() - startMs,
                                    null
                            );
                            log.info("VNPAY recurring execute success: status={}", response.statusCode());
                            return body;
                        }))
                .block();

        RecurringInitResult result = new RecurringInitResult();
        result.setRawResponse(responseJson);
        log.info("Recurring init response: {}", responseJson);
        if (responseJson == null || responseJson.isBlank()) {
            result.setSuccess(false);
            result.setRspCode("99");
            result.setRspMsg("Empty response from VNPAY");
            reg.setStatus(RecurringStatus.FAILED);
            reg.setRspCode("99");
            reg.setRspMsg("Empty response from VNPAY");
            recurringRepository.save(reg);
            return result;
        }

        JsonObject resp = JsonParser.parseString(responseJson).getAsJsonObject();
        String rspCode = getString(resp, "rspCode");
        String rspMsg = getString(resp, "rspMsg");
        String dataKey = getString(resp, "dataKey");
        String transactionId = null;
        if (resp.has("transaction") && resp.get("transaction").isJsonObject()) {
            transactionId = getString(resp.getAsJsonObject("transaction"), "id");
        }

        result.setRspCode(rspCode);
        result.setRspMsg(rspMsg);
        result.setDataKey(dataKey);
        result.setTransactionId(transactionId);
        result.setRecurringId(transactionId);  // transaction.id = Mã GD đăng ký định kỳ = recurringId (đặc tả 2.5.3.2)
        result.setSuccess("00".equals(rspCode));

        reg.setRspCode(rspCode);
        reg.setRspMsg(rspMsg);
        reg.setDataKey(dataKey);
        reg.setTransactionId(transactionId);
        reg.setRecurringId(transactionId);  // Lưu sớm để có recurringId ngay sau init (trước redirect)
        reg.setStatus("00".equals(rspCode) ? RecurringStatus.PENDING : RecurringStatus.FAILED);
        recurringRepository.save(reg);

        return result;
    }

    public RecurringActionResult recurringPay(RecurringPayRequest request) {
        String reqId = String.valueOf(System.currentTimeMillis());
        String mcDate = getNowGmt7("yyyyMMddHHmmss");
        String addData = defaultString(request.getAddData());

        JsonObject payload = new JsonObject();
        payload.addProperty("reqId", reqId);
        payload.addProperty("command", RECURRING_PAY_COMMAND);
        payload.addProperty("tmnCode", recurringConfig.getTmnCode());
        payload.addProperty("version", RECURRING_VERSION);
        payload.addProperty("addData", addData);

        JsonObject order = new JsonObject();
        order.addProperty("orderReference", request.getOrderReference());
        order.addProperty("orderInfo", request.getOrderInfo());
        payload.add("order", order);

        JsonObject transaction = new JsonObject();
        transaction.addProperty("recurringId", request.getRecurringId());
        transaction.addProperty("recurringAmount", request.getRecurringAmount());
        transaction.addProperty("recurringDate", request.getRecurringDate());
        transaction.addProperty("currCode", "VND");
        transaction.addProperty("mcDate", mcDate);
        payload.add("transaction", transaction);

        JsonObject token = new JsonObject();
        token.addProperty("tokenId", request.getTokenId());
        payload.add("token", token);

        String hashData = String.join("|",
                reqId,
                RECURRING_PAY_COMMAND,
                request.getOrderReference(),
                request.getOrderInfo(),
                recurringConfig.getTmnCode(),
                request.getTokenId(),
                request.getRecurringId(),
                String.valueOf(request.getRecurringAmount()),
                request.getRecurringDate(),
                "VND",
                addData,
                RECURRING_VERSION,
                mcDate
        );

        log.info("Recurring pay call: orderReference={}, recurringId={}",
                request.getOrderReference(), request.getRecurringId());
        return executeRecurringCommand(payload, hashData);
    }

    public RecurringActionResult updateToken(UpdateTokenRequest request, String clientIp, String userAgent) {
        String reqId = String.valueOf(System.currentTimeMillis());
        String mcDate = getNowGmt7("yyyyMMddHHmmss");
        String addData = defaultString(request.getAddData());

        JsonObject payload = new JsonObject();
        payload.addProperty("reqId", reqId);
        payload.addProperty("command", UPDATE_TOKEN_COMMAND);
        payload.addProperty("tmnCode", recurringConfig.getTmnCode());
        payload.addProperty("version", RECURRING_VERSION);
        payload.addProperty("addData", addData);
        payload.addProperty("ipAddr", clientIp);
        payload.addProperty("userAgent", userAgent);

        JsonObject order = new JsonObject();
        order.addProperty("orderReference", request.getOrderReference());
        order.addProperty("orderInfo", request.getOrderInfo());
        payload.add("order", order);

        JsonObject transaction = new JsonObject();
        transaction.addProperty("recurringId", request.getRecurringId());
        transaction.addProperty("returnUrl", recurringConfig.getReturnUrl());
        transaction.addProperty("cancelUrl", recurringConfig.getCancelUrl());
        transaction.addProperty("mcDate", mcDate);
        payload.add("transaction", transaction);

        String hashData = String.join("|",
                reqId,
                UPDATE_TOKEN_COMMAND,
                request.getOrderReference(),
                request.getOrderInfo(),
                recurringConfig.getTmnCode(),
                request.getRecurringId(),
                addData,
                clientIp,
                userAgent,
                recurringConfig.getReturnUrl(),
                recurringConfig.getCancelUrl(),
                RECURRING_VERSION,
                mcDate
        );

        log.info("Update token call: orderReference={}, recurringId={}",
                request.getOrderReference(), request.getRecurringId());
        return executeRecurringCommand(payload, hashData);
    }

    public RecurringActionResult updateRecurring(UpdateRecurringRequest request, String clientIp, String userAgent) {
        String reqId = String.valueOf(System.currentTimeMillis());
        String addData = defaultString(request.getAddData());

        JsonObject payload = new JsonObject();
        payload.addProperty("reqId", reqId);
        payload.addProperty("command", UPDATE_RECURRING_COMMAND);
        payload.addProperty("tmnCode", recurringConfig.getTmnCode());
        payload.addProperty("version", RECURRING_VERSION);
        payload.addProperty("addData", addData);
        payload.addProperty("ipAddr", clientIp);
        payload.addProperty("userAgent", userAgent);

        JsonObject transaction = new JsonObject();
        transaction.addProperty("recurringId", request.getRecurringId());
        transaction.addProperty("recurringNumber", request.getRecurringNumber());
        transaction.addProperty("recurringEndDate", request.getRecurringEndDate());
        transaction.addProperty("amount", request.getAmount());
        transaction.addProperty("currCode", "VND");
        payload.add("transaction", transaction);

        JsonObject token = new JsonObject();
        token.addProperty("tokenId", request.getTokenId());
        payload.add("token", token);

        String hashData = String.join("|",
                reqId,
                UPDATE_RECURRING_COMMAND,
                recurringConfig.getTmnCode(),
                request.getTokenId(),
                request.getRecurringId(),
                String.valueOf(request.getRecurringNumber()),
                request.getRecurringEndDate(),
                String.valueOf(request.getAmount()),
                "VND",
                addData,
                clientIp,
                userAgent,
                RECURRING_VERSION
        );

        log.info("Update recurring call: recurringId={}, recurringNumber={}",
                request.getRecurringId(), request.getRecurringNumber());
        return executeRecurringCommand(payload, hashData);
    }

    public RecurringActionResult cancelRecurring(CancelRecurringRequest request, String clientIp, String userAgent) {
        String reqId = String.valueOf(System.currentTimeMillis());
        String addData = defaultString(request.getAddData());

        JsonObject payload = new JsonObject();
        payload.addProperty("reqId", reqId);
        payload.addProperty("command", CANCEL_RECURRING_COMMAND);
        payload.addProperty("tmnCode", recurringConfig.getTmnCode());
        payload.addProperty("version", RECURRING_VERSION);
        payload.addProperty("addData", addData);
        payload.addProperty("ipAddr", clientIp);
        payload.addProperty("userAgent", userAgent);

        JsonObject transaction = new JsonObject();
        transaction.addProperty("recurringId", request.getRecurringId());
        payload.add("transaction", transaction);

        JsonObject token = new JsonObject();
        token.addProperty("tokenId", request.getTokenId());
        payload.add("token", token);

        String hashData = String.join("|",
                reqId,
                CANCEL_RECURRING_COMMAND,
                recurringConfig.getTmnCode(),
                request.getTokenId(),
                request.getRecurringId(),
                addData,
                clientIp,
                userAgent,
                RECURRING_VERSION
        );

        log.info("Cancel recurring call: recurringId={}", request.getRecurringId());
        return executeRecurringCommand(payload, hashData);
    }

    @Transactional
    public RecurringReturnResult processRecurringReturn(Map<String, String> params) {
        String secureHash = getParam(params, "vnp_secure_hash", "vnp_SecureHash");
        String orderRef = getParam(params, "vnp_txn_ref", "vnp_TxnRef");
        String responseCode = getParam(params, "vnp_response_code", "vnp_ResponseCode");
        String transactionStatus = getParam(params, "vnp_transaction_status", "vnp_TransactionStatus");
        String recurringId = getParam(params, "vnp_recurring_id", "vnp_RecurringId", "vnp_recurringId");

        String calculatedHash = buildSecureHashFromParams(params);
        if (!calculatedHash.equals(secureHash)) {
            log.warn("Recurring return invalid checksum. orderReference={}", orderRef);
            return new RecurringReturnResult(-1, null);
        }

        Optional<RecurringRegistration> regOpt = recurringRepository.findByOrderReference(orderRef);
        if (regOpt.isEmpty()) {
            return new RecurringReturnResult(0, recurringId);
        }

        RecurringRegistration reg = regOpt.get();
        if (recurringId != null && !recurringId.isBlank()) {
            reg.setRecurringId(recurringId);
        }
        String effectiveRecurringId = reg.getRecurringId();  // Đã có từ init (transaction.id) hoặc từ callback
        if (effectiveRecurringId == null || effectiveRecurringId.isBlank()) {
            effectiveRecurringId = recurringId;
        }
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            reg.setStatus(RecurringStatus.SUCCESS);
            reg.setRspCode(responseCode);
            reg.setRspMsg("SUCCESS");
            recurringRepository.save(reg);
            return new RecurringReturnResult(1, effectiveRecurringId);
        }

        reg.setStatus(RecurringStatus.FAILED);
        reg.setRspCode(responseCode);
        reg.setRspMsg("FAILED");
        recurringRepository.save(reg);
        return new RecurringReturnResult(0, effectiveRecurringId);
    }

    public record RecurringReturnResult(int status, String recurringId) {}

    @Transactional
    public Map<String, String> processRecurringIpn(Map<String, String> params) {
        try {
            String secureHash = getParam(params, "vnp_secure_hash", "vnp_SecureHash");
            String orderRef = getParam(params, "vnp_txn_ref", "vnp_TxnRef");
            String responseCode = getParam(params, "vnp_response_code", "vnp_ResponseCode");
            String transactionStatus = getParam(params, "vnp_transaction_status", "vnp_TransactionStatus");
            Long vnpAmount = parseLongSafe(getParam(params, "vnp_amount", "vnp_Amount"));

            String calculatedHash = buildSecureHashFromParams(params);
            if (!calculatedHash.equals(secureHash)) {
                return createIpnResponse("97", "Invalid Checksum");
            }

            Optional<RecurringRegistration> regOpt = recurringRepository.findByOrderReference(orderRef);
            if (regOpt.isEmpty()) {
                return createIpnResponse("01", "Order not Found");
            }

            RecurringRegistration reg = regOpt.get();
            // recurring flow lưu amount theo đơn vị minor (x100), giống vnp_amount trả về từ VNPAY.
            if (vnpAmount != null && reg.getAmount() > 0 && reg.getAmount() != vnpAmount) {
                return createIpnResponse("04", "Invalid Amount");
            }

            if (reg.getStatus() == RecurringStatus.SUCCESS) {
                return createIpnResponse("02", "Order already confirmed");
            }

            String recurringId = getParam(params, "vnp_recurring_id", "vnp_RecurringId", "vnp_recurringId");
            if (recurringId != null && !recurringId.isBlank()) {
                reg.setRecurringId(recurringId);
            }
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                reg.setStatus(RecurringStatus.SUCCESS);
                reg.setRspCode(responseCode);
                reg.setRspMsg("SUCCESS");
            } else {
                reg.setStatus(RecurringStatus.FAILED);
                reg.setRspCode(responseCode);
                reg.setRspMsg("FAILED");
            }
            recurringRepository.save(reg);
            return createIpnResponse("00", "Confirm Success");
        } catch (Exception ex) {
            log.error("Recurring IPN processing failed", ex);
            return createIpnResponse("99", "Unknown Error");
        }
    }

    private String authenticate() {
        long startMs = System.currentTimeMillis();
        JsonObject json = new JsonObject();
        json.addProperty("clientId", recurringConfig.getClientId());
        json.addProperty("username", recurringConfig.getUsername());
        json.addProperty("password", recurringConfig.getPassword());
        json.addProperty("clientSecret", recurringConfig.getClientSecret());

        log.info("Recurring auth call: endpoint=/oauth/authenticate, clientId={}, username={}",
                mask(recurringConfig.getClientId()),
                recurringConfig.getUsername());
        String url = recurringConfig.getApiUrl() + "/oauth/authenticate";
        String responseJson = recurringWebClient.post()
                .uri("/oauth/authenticate")
                .header("Content-Type", "application/json")
                .bodyValue(json.toString())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            if (response.statusCode().isError()) {
                                log.error("VNPAY auth error: status={}, body={}", response.statusCode(), body);
                            } else {
                                log.info("VNPAY auth success: status={}, body={}", response.statusCode(), body);
                            }
                            auditLogService.logOutbound(
                                    VNPayAuditFlow.RECURRING,
                                    "RECURRING_AUTH",
                                    null,
                                    url,
                                    "POST",
                                    json.toString(),
                                    response.statusCode().value(),
                                    body,
                                    System.currentTimeMillis() - startMs,
                                    response.statusCode().isError() ? "HTTP " + response.statusCode().value() : null
                            );
                            return body;
                        }))
                .block();

        if (responseJson == null || responseJson.isBlank()) {
            throw new IllegalStateException("Empty auth response from VNPAY.");
        }

        JsonObject resp = JsonParser.parseString(responseJson).getAsJsonObject();
        String rspCode = getString(resp, "rspCode");
        if (!"00".equals(rspCode)) {
            throw new IllegalStateException("Auth failed with rspCode=" + rspCode);
        }

        JsonObject data = resp.getAsJsonObject("data");
        String accessToken = getString(data, "accessToken");
        String tokenType = getString(data, "tokenType");
        return tokenType + " " + accessToken;
    }

    private RecurringActionResult executeRecurringCommand(JsonObject payload, String hashData) {
        String token = authenticate();
        String secureHash = VNPayUtils.hmacSHA512(recurringConfig.getSecretKey(), hashData);
        payload.addProperty("secureHash", secureHash);

        long startMs = System.currentTimeMillis();
        String url = recurringConfig.getApiUrl() + "/recurring-payment/execute";
        String responseJson = recurringWebClient.post()
                .uri("/recurring-payment/execute")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .bodyValue(payload.toString())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            if (response.statusCode().isError()) {
                                log.error("VNPAY recurring execute error: status={}, body={}", response.statusCode(), body);
                                auditLogService.logOutbound(
                                        VNPayAuditFlow.RECURRING,
                                        "RECURRING_EXECUTE",
                                        null,
                                        url,
                                        "POST",
                                        payload.toString(),
                                        response.statusCode().value(),
                                        body,
                                        System.currentTimeMillis() - startMs,
                                        "HTTP " + response.statusCode().value()
                                );
                                throw new IllegalStateException("VNPAY recurring execute failed: status="
                                        + response.statusCode().value());
                            }
                            auditLogService.logOutbound(
                                    VNPayAuditFlow.RECURRING,
                                    "RECURRING_EXECUTE",
                                    null,
                                    url,
                                    "POST",
                                    payload.toString(),
                                    response.statusCode().value(),
                                    body,
                                    System.currentTimeMillis() - startMs,
                                    null
                            );
                            return body;
                        }))
                .block();

        RecurringActionResult result = new RecurringActionResult();
        result.setRawResponse(responseJson);
        if (responseJson == null || responseJson.isBlank()) {
            result.setSuccess(false);
            result.setRspCode("99");
            result.setRspMsg("Empty response from VNPAY");
            return result;
        }

        JsonObject resp = JsonParser.parseString(responseJson).getAsJsonObject();
        String rspCode = getString(resp, "rspCode");
        String rspMsg = getString(resp, "rspMsg");
        result.setRspCode(rspCode);
        result.setRspMsg(rspMsg);
        result.setSuccess("00".equals(rspCode));
        return result;
    }

    private String buildInitHashData(
            String reqId,
            String command,
            String orderReference,
            String orderInfo,
            String orderType,
            String tmnCode,
            long recurringAmount,
            int recurringFrequencyNumber,
            String recurringFrequency,
            int recurringNumber,
            String recurringDate,
            String recurringStartDate,
            String recurringEndDate,
            long amount,
            String currCode,
            String addData,
            String appUserId,
            String customerForename,
            String customerSurname,
            String ipAddr,
            String userAgent,
            String returnUrl,
            String cancelUrl,
            String version,
            String locale,
            String mcDate) {
        return String.join("|",
                reqId,
                command,
                orderReference,
                orderInfo,
                orderType,
                tmnCode,
                String.valueOf(recurringAmount),
                String.valueOf(recurringFrequencyNumber),
                recurringFrequency,
                String.valueOf(recurringNumber),
                recurringDate,
                recurringStartDate,
                recurringEndDate,
                String.valueOf(amount),
                currCode,
                addData,
                appUserId,
                customerForename,
                customerSurname,
                ipAddr,
                userAgent,
                returnUrl,
                cancelUrl,
                version,
                locale,
                mcDate
        );
    }

    private String buildSecureHashFromParams(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (fieldName == null) {
                continue;
            }
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if ("vnp_secure_hash".equalsIgnoreCase(fieldName) || "vnp_secure_hash_type".equalsIgnoreCase(fieldName)) {
                continue;
            }
            String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
            sb.append(fieldName).append("=").append(encodedValue).append("&");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return VNPayUtils.hmacSHA512(recurringConfig.getSecretKey(), sb.toString());
    }

    private Map<String, String> createIpnResponse(String rspCode, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("RspCode", rspCode);
        response.put("Message", message);
        return response;
    }

    private String getNowGmt7(String pattern) {
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(cld.getTime());
    }

    private String getParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            if (params.containsKey(key)) {
                return params.get(key);
            }
        }
        return null;
    }

    private Long parseLongSafe(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        int len = value.length();
        if (len <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(len - 2);
    }
}
