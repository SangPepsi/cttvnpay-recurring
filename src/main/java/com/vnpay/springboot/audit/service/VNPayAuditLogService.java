package com.vnpay.springboot.audit.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vnpay.springboot.audit.entity.VNPayAuditDirection;
import com.vnpay.springboot.audit.entity.VNPayAuditFlow;
import com.vnpay.springboot.audit.entity.VNPayAuditLog;
import com.vnpay.springboot.audit.repository.VNPayAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class VNPayAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(VNPayAuditLogService.class);

    private final VNPayAuditLogRepository repo;

    public VNPayAuditLogService(VNPayAuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void logInbound(
            VNPayAuditFlow flow,
            String action,
            String correlationId,
            String url,
            String method,
            String remoteIp,
            String userAgent,
            Map<String, String> params
    ) {
        VNPayAuditLog entry = new VNPayAuditLog();
        entry.setFlow(flow);
        entry.setDirection(VNPayAuditDirection.INBOUND);
        entry.setAction(action);
        entry.setCorrelationId(correlationId);
        entry.setUrl(url);
        entry.setMethod(method);
        entry.setRemoteIp(remoteIp);
        entry.setUserAgent(userAgent);
        entry.setRequestData(params == null ? null : params.toString());
        repo.save(entry);
    }

    @Transactional
    public void logOutbound(
            VNPayAuditFlow flow,
            String action,
            String correlationId,
            String url,
            String method,
            String requestData,
            Integer responseStatus,
            String responseData,
            Long durationMs,
            String errorMessage
    ) {
        VNPayAuditLog entry = new VNPayAuditLog();
        entry.setFlow(flow);
        entry.setDirection(VNPayAuditDirection.OUTBOUND);
        entry.setAction(action);
        entry.setCorrelationId(correlationId);
        entry.setUrl(url);
        entry.setMethod(method);
        entry.setRequestData(sanitizeRequest(action, requestData));
        entry.setResponseStatus(responseStatus);
        entry.setResponseData(sanitizeResponse(action, responseData));
        entry.setDurationMs(durationMs);
        entry.setErrorMessage(errorMessage);
        repo.save(entry);
    }

    private String sanitizeRequest(String action, String requestData) {
        if (requestData == null || requestData.isBlank()) {
            return requestData;
        }
        // Mask sensitive fields for auth calls.
        if (action != null && action.toUpperCase().contains("AUTH")) {
            try {
                JsonObject obj = JsonParser.parseString(requestData).getAsJsonObject();
                mask(obj, "password");
                mask(obj, "clientSecret");
                mask(obj, "secretKey");
                return obj.toString();
            } catch (Exception ex) {
                log.debug("Audit sanitize failed, storing raw requestData.");
                return requestData;
            }
        }
        return requestData;
    }

    private String sanitizeResponse(String action, String responseData) {
        if (responseData == null || responseData.isBlank()) {
            return responseData;
        }
        if (action != null && action.toUpperCase().contains("AUTH")) {
            try {
                JsonObject obj = JsonParser.parseString(responseData).getAsJsonObject();
                if (obj.has("data") && obj.get("data").isJsonObject()) {
                    JsonObject data = obj.getAsJsonObject("data");
                    mask(data, "accessToken");
                    mask(data, "refreshToken");
                }
                return obj.toString();
            } catch (Exception ex) {
                return responseData;
            }
        }
        return responseData;
    }

    private void mask(JsonObject obj, String field) {
        if (obj == null || field == null) {
            return;
        }
        if (obj.has(field)) {
            obj.addProperty(field, "****");
        }
    }
}

