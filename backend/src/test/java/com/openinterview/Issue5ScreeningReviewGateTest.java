package com.openinterview;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.service.InMemoryWorkflowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Issue5ScreeningReviewGateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryWorkflowService workflowService;

    @Test
    void screenResultShouldContainHumanReviewGateFlag() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "issue5-case-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":30001,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiSuggestionOnly").value(true))
                .andExpect(jsonPath("$.data.reasonSummary").exists());

        mockMvc.perform(get("/api/v1/candidate/resume/screen/result/30001")
                        .param("jobCode", "JAVA_ADV_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewResult").isEmpty());
    }

    @Test
    void reviewWithInvalidReviewResultShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "issue5-case-02-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":30002,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "issue5-case-02-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":30002,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":4,\"reviewComment\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void screenStatusIllegalTransitionShouldFail() {
        workflowService.createOrGetScreenResult(30003L, "JAVA_ADV_01", "issue5-case-03-a");
        workflowService.markScreenSuccess(30003L, "JAVA_ADV_01", 80.0, 2);
        ApiException ex = Assertions.assertThrows(
                ApiException.class,
                () -> workflowService.markScreenFailed(30003L, "JAVA_ADV_01", "should-fail")
        );
        Assertions.assertEquals(ErrorCode.SCREEN_STATUS_ILLEGAL, ex.getErrorCode());
    }

    @Test
    void concurrentScreenWithSameIdempotencyKeyShouldReturnSameTaskCode() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                calls.add(() -> {
                    MvcResult result = mockMvc.perform(post("/api/v1/candidate/resume/screen")
                                    .header("X-Idempotency-Key", "issue5-case-04")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"candidateId\":30004,\"jobCode\":\"JAVA_ADV_01\"}"))
                            .andExpect(status().isOk())
                            .andReturn();
                    return extract(result.getResponse().getContentAsString(), "\"taskCode\":\"", "\"");
                });
            }
            List<Future<String>> futures = executor.invokeAll(calls);
            String expected = futures.get(0).get();
            for (Future<String> future : futures) {
                Assertions.assertEquals(expected, future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void keyActionsShouldBeRecordedInAuditTrail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "issue5-case-06-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":30006,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "issue5-case-06-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":30006,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":1,\"reviewComment\":\"pass\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/internal/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditLogs").isArray())
                .andExpect(jsonPath("$.data.auditLogs[0].module").value("candidate"))
                .andExpect(jsonPath("$.data.auditLogs[0].traceId").exists())
                .andExpect(jsonPath("$.data.auditLogs[0].bizCode").exists())
                .andExpect(jsonPath("$.data.auditLogs[0].errorCode").exists());
    }

    private String extract(String body, String left, String right) {
        int start = body.indexOf(left);
        if (start < 0) {
            return "";
        }
        int from = start + left.length();
        int end = body.indexOf(right, from);
        if (end < 0) {
            end = body.length();
        }
        return body.substring(from, end);
    }
}
