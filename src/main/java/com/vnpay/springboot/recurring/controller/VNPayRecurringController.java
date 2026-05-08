package com.vnpay.springboot.recurring.controller;

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
import com.vnpay.springboot.recurring.service.VNPayRecurringService;
import com.vnpay.springboot.Util.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/vnpay/recurring")
public class VNPayRecurringController {

    private final VNPayRecurringService recurringService;
    private final VNPayRecurringConfig recurringConfig;
    private final VNPayAuditLogService auditLogService;
    public VNPayRecurringController(
            VNPayRecurringService recurringService,
            VNPayRecurringConfig recurringConfig,
            VNPayAuditLogService auditLogService) {
        this.recurringService = recurringService;
        this.recurringConfig = recurringConfig;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/init")
    public String showInitForm(Model model) {
        model.addAttribute("defaultReturnUrl", recurringConfig.getReturnUrl());
        model.addAttribute("defaultCancelUrl", recurringConfig.getCancelUrl());
        return "vnpay_recurring_init";
    }

    @PostMapping("/init")
    public String initRecurring(
            @Valid RecurringInitRequest initRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin đăng ký định kỳ.");
            return "vnpay_recurring_init";
        }

        String clientIp = VNPayUtils.getIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown";
        }

        try {
            RecurringInitResult result = recurringService.initRecurring(initRequest, clientIp, userAgent);
            model.addAttribute("result", result);
            model.addAttribute("tmnCode", recurringConfig.getTmnCode());
            model.addAttribute("payUrl", recurringConfig.getPayUrl());
            return "vnpay_recurring_init_result";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "vnpay_recurring_init";
        }
    }

    @GetMapping("/return")
    public String handleReturn(HttpServletRequest request, Model model) {
        Map<String, String> params = collectParams(request);
        auditLogService.logInbound(
                VNPayAuditFlow.RECURRING,
                "RECURRING_RETURN",
                getCorrId(params),
                VNPayUtils.getFullRequestUrl(request),
                request.getMethod(),
                VNPayUtils.getIpAddress(request),
                getUserAgent(request),
                params
        );
        var returnResult = recurringService.processRecurringReturn(params);

        model.addAttribute("orderRef", getParam(params, "vnp_txn_ref", "vnp_TxnRef"));
        model.addAttribute("amount", getParam(params, "vnp_amount", "vnp_Amount"));
        model.addAttribute("responseCode", getParam(params, "vnp_response_code", "vnp_ResponseCode"));
        model.addAttribute("transactionStatus", getParam(params, "vnp_transaction_status", "vnp_TransactionStatus"));
        model.addAttribute("recurringId", returnResult.recurringId());
        model.addAttribute("rawParams", params);
        model.addAttribute("signatureValid", returnResult.status() != -1);

        if (returnResult.status() == -1) {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Lỗi xác thực dữ liệu (Invalid Signature).");
        } else if (returnResult.status() == 1) {
            model.addAttribute("status", "success");
            model.addAttribute("message", "Đăng ký thanh toán định kỳ thành công!");
        } else {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Giao dịch không thành công.");
        }

        return "vnpay_recurring_return";
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Map<String, String> handleIpn(HttpServletRequest request) {
        Map<String, String> params = collectParams(request);
        auditLogService.logInbound(
                VNPayAuditFlow.RECURRING,
                "RECURRING_IPN",
                getCorrId(params),
                VNPayUtils.getFullRequestUrl(request),
                request.getMethod(),
                VNPayUtils.getIpAddress(request),
                getUserAgent(request),
                params
        );
        return recurringService.processRecurringIpn(params);
    }

    @GetMapping("/pay")
    public String showPayForm(
            @RequestParam(required = false) String recurringId,
            @RequestParam(required = false) String orderReference,
            Model model) {
        if (recurringId != null) model.addAttribute("prefillRecurringId", recurringId);
        if (orderReference != null) model.addAttribute("prefillOrderReference", orderReference);
        return "vnpay_recurring_pay";
    }

    @PostMapping("/pay")
    public String recurringPay(
            @Valid RecurringPayRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin thanh toán định kỳ.");
            return "vnpay_recurring_pay";
        }
        RecurringActionResult result = recurringService.recurringPay(request);
        return renderActionResult(model, "Thanh toán định kỳ", result, "/vnpay/recurring/pay");
    }

    @GetMapping("/update-token")
    public String showUpdateTokenForm() {
        return "vnpay_recurring_update_token";
    }

    @PostMapping("/update-token")
    public String updateToken(
            @Valid UpdateTokenRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin cập nhật thẻ.");
            return "vnpay_recurring_update_token";
        }
        String clientIp = VNPayUtils.getIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        RecurringActionResult result = recurringService.updateToken(request, clientIp, userAgent);
        return renderActionResult(model, "Cập nhật thẻ/token", result, "/vnpay/recurring/update-token");
    }

    @GetMapping("/update-recurring")
    public String showUpdateRecurringForm() {
        return "vnpay_recurring_update_recurring";
    }

    @PostMapping("/update-recurring")
    public String updateRecurring(
            @Valid UpdateRecurringRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin cập nhật số kỳ.");
            return "vnpay_recurring_update_recurring";
        }
        String clientIp = VNPayUtils.getIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        RecurringActionResult result = recurringService.updateRecurring(request, clientIp, userAgent);
        return renderActionResult(model, "Cập nhật số kỳ", result, "/vnpay/recurring/update-recurring");
    }

    @GetMapping("/cancel")
    public String showCancelForm() {
        return "vnpay_recurring_cancel";
    }

    @PostMapping("/cancel")
    public String cancelRecurring(
            @Valid CancelRecurringRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin hủy đăng ký.");
            return "vnpay_recurring_cancel";
        }
        String clientIp = VNPayUtils.getIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        RecurringActionResult result = recurringService.cancelRecurring(request, clientIp, userAgent);
        return renderActionResult(model, "Hủy đăng ký", result, "/vnpay/recurring/cancel");
    }

    private Map<String, String> collectParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }

    private String getParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            if (params.containsKey(key)) {
                return params.get(key);
            }
        }
        return null;
    }

    private String getCorrId(Map<String, String> params) {
        return getParam(params, "vnp_txn_ref", "vnp_TxnRef");
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "Unknown" : userAgent;
    }

    private String renderActionResult(Model model, String title, RecurringActionResult result, String backLink) {
        model.addAttribute("title", title);
        model.addAttribute("result", result);
        model.addAttribute("backLink", backLink);
        return "vnpay_recurring_action_result";
    }
}
