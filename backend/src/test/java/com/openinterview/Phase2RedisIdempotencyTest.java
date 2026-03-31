package com.openinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "openinterview.redis.enabled=true"
})
@AutoConfigureMockMvc
class Phase2RedisIdempotencyTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentSameKeyShouldReturnSameTaskCodeAnd200() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                calls.add(() -> {
                    MvcResult result = mockMvc.perform(post("/api/v1/candidate/resume/screen")
                                    .header("X-Idempotency-Key", "phase2-redis-idem-01")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"candidateId\":40001,\"jobCode\":\"JAVA_ADV_01\"}"))
                            .andExpect(status().isOk())
                            .andReturn();
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    return root.path("data").path("taskCode").asText();
                });
            }
            List<Future<String>> futures = executor.invokeAll(calls);
            String expected = futures.get(0).get();
            Assertions.assertTrue(expected != null && !expected.isBlank());
            for (Future<String> f : futures) {
                Assertions.assertEquals(expected, f.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}

