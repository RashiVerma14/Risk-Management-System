package com.riskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskmanager.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Fallback in-memory rate limiter if Redis is offline
    private final Map<String, SlidingWindow> inMemoryWindows = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 120;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate limit API mutations or public auth endpoints
        if (path.startsWith("/api/auth/") || (path.startsWith("/api/") && !"GET".equalsIgnoreCase(request.getMethod()))) {
            String clientIp = getClientIp(request);
            String rateLimitKey = "ratelimit:" + clientIp + ":" + (path.startsWith("/api/auth/") ? "auth" : "api");

            boolean allowed = checkRateLimit(rateLimitKey);

            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<Object> err = ApiResponse.error("Too many requests. Please slow down and try again later.", path);
                response.getWriter().write(objectMapper.writeValueAsString(err));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return count != null && count <= MAX_REQUESTS_PER_MINUTE;
        } catch (Exception ex) {
            // Graceful fallback to local in-memory rate limiting if Redis connection drops
            long currentMinute = System.currentTimeMillis() / 60000;
            SlidingWindow window = inMemoryWindows.compute(key, (k, v) -> {
                if (v == null || v.minute != currentMinute) {
                    return new SlidingWindow(currentMinute, new AtomicInteger(1));
                }
                v.counter.incrementAndGet();
                return v;
            });
            return window.counter.get() <= MAX_REQUESTS_PER_MINUTE;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record SlidingWindow(long minute, AtomicInteger counter) {}
}
