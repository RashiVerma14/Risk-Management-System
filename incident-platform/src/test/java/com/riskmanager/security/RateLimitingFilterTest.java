package com.riskmanager.security;

import com.riskmanager.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RateLimitingFilterTest {

    @Test
    void testRateLimiterAllowsUnderThresholdWithInMemoryFallback() throws ServletException, IOException {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RateLimitingFilter filter = new RateLimitingFilter(redis);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/incidents");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        // Expect 200 OK because request is under 120 req/min
        assertEquals(200, response.getStatus());
    }

    @Test
    void testRateLimiterBlocksWhenExceedingLimit() throws ServletException, IOException {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RateLimitingFilter filter = new RateLimitingFilter(redis);

        // Fire 125 POST requests in the same minute
        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 125; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/incidents");
            request.setRemoteAddr("10.0.0.50");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        // The 125th request exceeds 120 req/min limit
        assertEquals(429, lastResponse.getStatus());
    }
}
