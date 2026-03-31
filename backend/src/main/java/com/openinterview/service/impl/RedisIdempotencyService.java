package com.openinterview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.service.IdempotencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(value = "openinterview.redis.enabled", havingValue = "true")
public class RedisIdempotencyService implements IdempotencyService {

    private static final String PREFIX_LOCK = "idem:lock:";
    private static final String PREFIX_VAL = "idem:val:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        String lockKey = PREFIX_LOCK + key;
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1", ttl);
            return Boolean.TRUE.equals(ok);
        } catch (DataAccessException e) {
            return false;
        }
    }

    @Override
    public <T> T getOrCompute(String key, Duration ttl, Class<T> type, Supplier<T> supplier) {
        String valKey = PREFIX_VAL + key;
        try {
            String cached = redis.opsForValue().get(valKey);
            if (cached != null && !cached.isBlank()) {
                return readValue(cached, type);
            }
        } catch (DataAccessException e) {
            return supplier.get();
        }

        Duration lockTtl = ttl.compareTo(Duration.ofSeconds(3)) > 0 ? Duration.ofSeconds(3) : ttl;
        boolean acquired = tryAcquire(key, lockTtl);
        if (!acquired) {
            T waited = waitValue(valKey, type, Duration.ofSeconds(2));
            if (waited != null) {
                return waited;
            }
        }

        T out = supplier.get();
        String payload = writeValue(out);
        try {
            redis.opsForValue().set(valKey, payload, ttl);
        } catch (DataAccessException e) {
            // 不影响主流程：即使 Redis 写入失败，也返回本次计算结果
        }
        return out;
    }

    private <T> T waitValue(String valKey, Class<T> type, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            String v = redis.opsForValue().get(valKey);
            if (v != null && !v.isBlank()) {
                return readValue(v, type);
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("idempotency deserialize failed", e);
        }
    }

    private String writeValue(Object v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (Exception e) {
            throw new IllegalStateException("idempotency serialize failed", e);
        }
    }
}

