package com.demo.upimesh.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@Profile("redis")
public class RedisIdempotencyService implements IdempotencyService {

    private static final String KEY_PREFIX = "upimesh:idempotency:";

    @Autowired private StringRedisTemplate redis;

    @Value("${upi.mesh.idempotency-ttl-seconds:86400}")
    private long ttlSeconds;

    @Override
    public boolean claim(String packetHash) {
        Boolean firstClaim = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + packetHash, "1", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(firstClaim);
    }

    @Override
    public int size() {
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    @Override
    public void clear() {
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }
}