package com.openinterview.service.impl;

import com.openinterview.service.TokenBlacklistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(value = "openinterview.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Long> expirations = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String tokenId, Duration ttl) {
        expirations.put(tokenId, System.currentTimeMillis() + Math.max(0, ttl.toMillis()));
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        Long exp = expirations.get(tokenId);
        if (exp == null) {
            return false;
        }
        if (exp < System.currentTimeMillis()) {
            expirations.remove(tokenId);
            return false;
        }
        return true;
    }
}

