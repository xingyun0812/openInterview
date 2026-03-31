package com.openinterview.service.impl;

import com.openinterview.service.TokenBlacklistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@ConditionalOnProperty(value = "openinterview.redis.enabled", havingValue = "true")
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String PREFIX = "token:blacklist:";

    private final StringRedisTemplate redis;

    public RedisTokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void blacklist(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        try {
            redis.opsForValue().set(PREFIX + tokenId, "1", ttl);
        } catch (DataAccessException e) {
            // best-effort
        }
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        try {
            Boolean has = redis.hasKey(PREFIX + tokenId);
            return Boolean.TRUE.equals(has);
        } catch (DataAccessException e) {
            return false;
        }
    }
}

