package com.vnpay.springboot.Util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VNPayUtilsTest {

    @Test
    void hmacSHA512_shouldBeDeterministic() {
        String key = "secret123";
        String data = "vnp_Amount=100000&vnp_TxnRef=TEST001";
        String hash1 = VNPayUtils.hmacSHA512(key, data);
        String hash2 = VNPayUtils.hmacSHA512(key, data);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).matches("^[0-9a-f]{128}$");
    }

    @Test
    void hmacSHA512_shouldProduceDifferentHashForDifferentData() {
        String key = "secret123";
        String hash1 = VNPayUtils.hmacSHA512(key, "data1");
        String hash2 = VNPayUtils.hmacSHA512(key, "data2");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hmacSHA512_shouldProduceDifferentHashForDifferentKeys() {
        String data = "same-data";
        String hash1 = VNPayUtils.hmacSHA512("key1", data);
        String hash2 = VNPayUtils.hmacSHA512("key2", data);
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hmacSHA512_shouldReturnEmptyWhenKeyNull() {
        assertThat(VNPayUtils.hmacSHA512(null, "data")).isEmpty();
    }

    @Test
    void hmacSHA512_shouldReturnEmptyWhenDataNull() {
        assertThat(VNPayUtils.hmacSHA512("key", null)).isEmpty();
    }

    @Test
    void getIpAddress_shouldUseXForwardedForWhenPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        assertThat(VNPayUtils.getIpAddress(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void getIpAddress_shouldUseFirstIpFromXForwardedForWhenMultiple() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("192.168.1.1, 10.0.0.1");
        assertThat(VNPayUtils.getIpAddress(request)).isEqualTo("192.168.1.1");
    }

    @Test
    void getIpAddress_shouldFallbackToRemoteAddrWhenNoForwardedHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.0.5");
        assertThat(VNPayUtils.getIpAddress(request)).isEqualTo("192.168.0.5");
    }

    @Test
    void getIpAddress_shouldMapIpv6LocalhostTo127_0_0_1() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        assertThat(VNPayUtils.getIpAddress(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void getRandomNumber_shouldReturnCorrectLength() {
        for (int len : new int[]{1, 5, 10, 20}) {
            String result = VNPayUtils.getRandomNumber(len);
            assertThat(result).hasSize(len);
            assertThat(result).matches("^[0-9]+$");
        }
    }

    @Test
    void getRandomNumber_shouldBeNumericOnly() {
        String result = VNPayUtils.getRandomNumber(100);
        assertThat(result).matches("^[0-9]{100}$");
    }
}
