package com.openinterview.service.impl;

import com.openinterview.service.IdempotencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(value = "openinterview.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryIdempotencyService implements IdempotencyService {

    private final Map<String, Long> expirations = new ConcurrentHashMap<>();
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long exp = now + Math.max(0, ttl.toMillis());
        Long prev = expirations.putIfAbsent(key, exp);
        if (prev == null) {
            return true;
        }
        if (prev < now) {
            expirations.put(key, exp);
            cache.remove(key);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Duration ttl, Class<T> type, Supplier<T> supplier) {
        long now = System.currentTimeMillis();
        Long exp = expirations.get(key);
        if (exp != null && exp >= now) {
            Object v = cache.get(key);
            if (v != null && type.isInstance(v)) {
                return (T) v;
            }
        }
        if (!tryAcquire(key, ttl)) {
            Object v = cache.get(key);
            if (v != null && type.isInstance(v)) {
                return (T) v;
            }
        }
        T out = supplier.get();
        expirations.put(key, System.currentTimeMillis() + Math.max(0, ttl.toMillis()));
        cache.put(key, out);
        return out;
    }
}

