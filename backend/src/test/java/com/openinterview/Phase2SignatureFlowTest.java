package com.openinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.signature.SignatureStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase2SignatureFlowTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateSignVerifyArchiveHappyPath() throws Exception {
        MvcResult gen = mockMvc.perform(post("/api/v2/signatures/generate")
                        .header("X-Idempotency-Key", "idem-sig-gen-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":99001,\"signUserId\":1,\"signUserName\":\"admin\",\"signType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(SignatureStatus.PENDING))
                .andReturn();
        long signatureId = MAPPER.readTree(gen.getResponse().getContentAsString()).path("data").path("signatureId").asLong();

        mockMvc.perform(post("/api/v2/signatures/" + signatureId + "/sign")
                        .header("X-Idempotency-Key", "idem-sig-sign-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signImgUrl\":\"https://mock/sign.png\",\"signIp\":\"127.0.0.1\",\"deviceInfo\":\"JUnit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(SignatureStatus.SIGNED))
                .andExpect(jsonPath("$.data.fileHash").isNotEmpty());

        mockMvc.perform(get("/api/v2/signatures/" + signatureId + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(jsonPath("$.data.status").value(SignatureStatus.VERIFIED));

        MvcResult ar = mockMvc.perform(post("/api/v2/signatures/" + signatureId + "/archive")
                        .header("X-Idempotency-Key", "idem-sig-arch-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(SignatureStatus.ARCHIVED))
                .andExpect(jsonPath("$.data.archivePath").isNotEmpty())
                .andReturn();
        String path = MAPPER.readTree(ar.getResponse().getContentAsString()).path("data").path("archivePath").asText();
        assertTrue(Files.exists(Path.of(path)));
    }

    @Test
    void illegalTransitionsReturn400() throws Exception {
        MvcResult gen = mockMvc.perform(post("/api/v2/signatures/generate")
                        .header("X-Idempotency-Key", "idem-sig-gen-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88001,\"signUserId\":2,\"signUserName\":\"u2\",\"signType\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        long signatureId = MAPPER.readTree(gen.getResponse().getContentAsString()).path("data").path("signatureId").asLong();

        mockMvc.perform(get("/api/v2/signatures/" + signatureId + "/verify"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v2/signatures/" + signatureId + "/archive")
                        .header("X-Idempotency-Key", "idem-sig-arch-02"))
                .andExpect(status().isBadRequest());
    }
}

