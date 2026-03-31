package com.openinterview.service;

import java.time.Duration;
import java.util.function.Supplier;

public interface IdempotencyService {
    boolean tryAcquire(String key, Duration ttl);

    <T> T getOrCompute(String key, Duration ttl, Class<T> type, Supplier<T> supplier);
}

