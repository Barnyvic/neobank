package com.vaultpay.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RateLimitKeyResolver {

    public enum Group {
        LOGIN,
        PUBLIC,
        AUTHENTICATED
    }

    public Optional<Group> resolveGroup(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/swagger-ui") || path.startsWith("/api-docs") || path.startsWith("/actuator")) {
            return Optional.empty();
        }

        if (!path.startsWith("/api/")) {
            return Optional.empty();
        }

        if (path.equals("/api/v1/auth/login") && HttpMethod.POST.matches(method)) {
            return Optional.of(Group.LOGIN);
        }

        if (path.startsWith("/api/v1/auth/")) {
            return Optional.of(Group.PUBLIC);
        }

        if (path.equals("/api/v1/paystack/webhook") && HttpMethod.POST.matches(method)) {
            return Optional.of(Group.PUBLIC);
        }

        return Optional.of(Group.AUTHENTICATED);
    }
}

