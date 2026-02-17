package com.vaultpay.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaultpay.common.dto.ApiResponse;
import com.vaultpay.common.exception.ErrorCode;
import com.vaultpay.common.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) 
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RETRY_AFTER = "Retry-After";

    private final LettuceBasedProxyManager proxyManager;
    private final RateLimitProperties properties;
    private final ClientIdentityResolver identityResolver;
    private final RateLimitKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            LettuceBasedProxyManager proxyManager,
            RateLimitProperties properties,
            ClientIdentityResolver identityResolver,
            RateLimitKeyResolver keyResolver,
            ObjectMapper objectMapper
    ) {
        this.proxyManager = proxyManager;
        this.properties = properties;
        this.identityResolver = identityResolver;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitKeyResolver.Group> groupOpt = keyResolver.resolveGroup(request);
        if (groupOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = identityResolver.resolveClientIp(request);
        byte[] key = buildKey(groupOpt.get(), clientIp);

        BucketConfiguration configuration = bucketConfigurationFor(groupOpt.get());
        Bucket bucket = proxyManager.builder().build(key, configuration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long nanosToWait = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(nanosToWait).toSeconds());
        response.setStatus(429);
        response.setHeader(RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Too many requests")
                .errorCode(ErrorCode.RATE_LIMITED.name())
                .details(Map.of("retryAfterSeconds", retryAfterSeconds))
                .path(request.getRequestURI())
                .requestId(MDC.get(MdcKeys.REQUEST_ID))
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private byte[] buildKey(RateLimitKeyResolver.Group group, String clientIp) {
        String key;
        if (group == RateLimitKeyResolver.Group.LOGIN) {
            key = "login:ip:" + clientIp;
        } else if (group == RateLimitKeyResolver.Group.PUBLIC) {
            key = "public:ip:" + clientIp;
        } else {
            Optional<String> userKey = identityResolver.resolveAuthenticatedUserKey();
            key = userKey.map(s -> "auth:user:" + s).orElse("auth:ip:" + clientIp);
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private BucketConfiguration bucketConfigurationFor(RateLimitKeyResolver.Group group) {
        long rpm;
        if (group == RateLimitKeyResolver.Group.LOGIN) {
            rpm = properties.getLogin().getRequestsPerMinute();
        } else if (group == RateLimitKeyResolver.Group.PUBLIC) {
            rpm = properties.getPublicRate().getRequestsPerMinute();
        } else {
            rpm = properties.getAuth().getRequestsPerMinute();
        }
        Bandwidth limit = Bandwidth.simple(rpm, Duration.ofMinutes(1));
        return BucketConfiguration.builder().addLimit(limit).build();
    }
}

