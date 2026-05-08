package com.vnpay.springboot.Config;

import com.vnpay.springboot.Util.VNPayUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter ghi log ngay khi có request tới IPN URL hoặc Return URL.
 * Giúp xác nhận VNPAY đã gọi tới (thấy log trong console).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VNPayCallbackLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(VNPayCallbackLoggingFilter.class);

    private static final List<String> CALLBACK_PATHS = Arrays.asList(
            "/vnpay/vnpay-return", "/vnpay/vnpay-ipn",
            "/vnpay/token/return", "/vnpay/token/ipn",
            "/vnpay/installment/return", "/vnpay/installment/ipn",
            "/vnpay/recurring/return", "/vnpay/recurring/ipn",
            "/vnpay/preauth/return", "/vnpay/preauth/ipn"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (isCallbackPath(path)) {
            String fullUrl = VNPayUtils.getFullRequestUrl(httpRequest);
            String ip = VNPayUtils.getIpAddress(httpRequest);
            String type = path.contains("ipn") ? "IPN" : "RETURN";
            log.info("VNPAY {} received: method={} url={} ip={}", type, method, fullUrl, ip);
        }
        chain.doFilter(request, response);
    }

    private boolean isCallbackPath(String path) {
        return CALLBACK_PATHS.stream().anyMatch(path::endsWith);
    }
}
