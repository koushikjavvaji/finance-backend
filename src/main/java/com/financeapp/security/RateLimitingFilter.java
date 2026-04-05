package com.financeapp.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiter using the token-bucket algorithm (Bucket4j).
 *
 * Limits:
 *   - Auth endpoints (/api/auth/**) → 10 requests / minute   (brute-force protection)
 *   - All other endpoints           → 100 requests / minute  (general API protection)
 *
 * Disabled in the "test" profile so integration tests are not throttled.
 * Each unique IP gets its own bucket. Buckets are stored in a ConcurrentHashMap
 * (suitable for single-instance deployments; swap for a distributed cache in prod).
 */
@Slf4j
@Component
@Profile("!test")   // ← not active when running with the "test" Spring profile
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String  clientIp   = resolveClientIp(request);
        boolean isAuthPath = request.getRequestURI().startsWith("/api/auth/");

        Bucket bucket = isAuthPath
                ? authBuckets.computeIfAbsent(clientIp, k -> buildAuthBucket())
                : generalBuckets.computeIfAbsent(clientIp, k -> buildGeneralBucket());

        if (bucket.tryConsume(1)) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {
                      "success": false,
                      "message": "Too many requests. Please slow down and try again later.",
                      "timestamp": "%s"
                    }
                    """.formatted(java.time.LocalDateTime.now()));
        }
    }

    /** Auth endpoints: 10 requests per minute (brute-force protection) */
    private Bucket buildAuthBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /** General API: 100 requests per minute per IP */
    private Bucket buildGeneralBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
