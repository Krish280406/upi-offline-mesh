package com.demo.upimesh.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BridgeRateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"/api/bridge/ingest".equals(request.getRequestURI())) return true;

        String bridgeNodeId = request.getHeader("X-Bridge-Node-Id");
        if (bridgeNodeId == null) bridgeNodeId = "unknown";

        Bucket bucket = buckets.computeIfAbsent(bridgeNodeId, id ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(20)
                                .refillGreedy(20, Duration.ofMinutes(1))
                                .build())
                        .build());

        if (bucket.tryConsume(1)) return true;

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"outcome\":\"RATE_LIMITED\",\"reason\":\"too_many_requests\"}");
        return false;
    }
}