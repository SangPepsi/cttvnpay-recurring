package com.vnpay.springboot.Util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;

public final class VNPayUtils {

    private VNPayUtils() {
    }

    /** Trả về full request URL kèm query string (nếu có). */
    public static String getFullRequestUrl(HttpServletRequest request) {
        String base = request.getRequestURL().toString();
        String qs = request.getQueryString();
        return (qs != null && !qs.isBlank()) ? base + "?" + qs : base;
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress != null && ipAddress.length() > 0 && !"unknown".equalsIgnoreCase(ipAddress)) {
                if (ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            if (ipAddress != null && ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress))) {
                ipAddress = "127.0.0.1";
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        return ipAddress;
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ------------------- VNPAY IP WHITELIST -------------------

    // Hợp nhất IP sandbox + production do VNPAY cung cấp.
    private static final Set<String> TRUSTED_VNPAY_IPS = Set.of(
            // Sandbox
            "113.160.92.202",
            "203.205.17.226",
            "103.220.84.4",
            // Production
            "113.52.45.78",
            "116.97.245.130",
            "42.118.107.252",
            "113.20.97.250",
            "203.171.19.146",
            "103.220.87.4",
            "103.220.86.4",
            "103.220.86.10",
            "103.220.87.10",
            "103.220.86.139",
            "103.220.87.139"
    );

    /**
     * Kiểm tra IP có thuộc whitelist VNPAY hay không.
     */
    public static boolean isTrustedVnPayIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        // getIpAddress đã chuẩn hóa IPv6 loopback về 127.0.0.1 nên chỉ cần so sánh chuỗi.
        return TRUSTED_VNPAY_IPS.contains(ipAddress.trim());
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            final SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
