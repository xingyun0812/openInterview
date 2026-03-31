package com.openinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase2EvaluationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createInterviewPlanBody(long candidateId) {
        return """
                {
                  "candidateId": %d,
                  "applyPosition": "JAVA_ADV",
                  "interviewRound": "R1",
                  "interviewType": 1,
                  "templateId": 1,
                  "interviewStartTime": "2026-08-01T09:00:00",
                  "interviewEndTime": "2026-08-01T11:00:00",
                  "interviewRoomId": "room-eval",
                  "interviewRoomLink": "https://meet.example/eval",
                  "hrUserId": 10,
                  "interviewerIds": "20,21"
                }
                """.formatted(candidateId);
    }

    private static String draftBody(long interviewId, long interviewerId, String interviewerName, String totalScore) {
        return """
                {
                  "interviewId": %d,
                  "interviewerId": %d,
                  "interviewerName": "%s",
                  "totalScore": %s,
                  "interviewResult": 1,
                  "advantageComment": "优势",
                  "disadvantageComment": "不足",
                  "comprehensiveComment": "总体评价",
                  "details": [
                    {"itemId": 1, "itemName": "基础", "itemFullScore": 50, "score": 40.00, "itemComment": "OK", "aiScorePreview": 45.00},
                    {"itemId": 2, "itemName": "项目", "itemFullScore": 50, "score": 42.00, "itemComment": "OK", "aiScorePreview": 44.00}
                  ]
                }
                """.formatted(interviewId, interviewerId, interviewerName, totalScore);
    }

    @Test
    void evaluationDraftSubmitSummaryReviewFlow() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", "idem-eval-plan-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInterviewPlanBody(70001L)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode plan = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
        long interviewId = plan.path("id").asLong();

        mockMvc.perform(post("/api/v2/evaluations/submit")
                        .header("X-Idempotency-Key", "idem-eval-submit-before-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":" + interviewId + ",\"interviewerId\":20}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v2/evaluations/draft")
                        .header("X-Idempotency-Key", "idem-eval-draft-20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody(interviewId, 20, "Alice", "82.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitStatus").value(0))
                .andExpect(jsonPath("$.data.aiSuggestionOnly").value(true));

        mockMvc.perform(post("/api/v2/evaluations/submit")
                        .header("X-Idempotency-Key", "idem-eval-submit-20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":" + interviewId + ",\"interviewerId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitStatus").value(1))
                .andExpect(jsonPath("$.data.submitTime").exists());

        mockMvc.perform(post("/api/v2/evaluations/draft")
                        .header("X-Idempotency-Key", "idem-eval-draft-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody(interviewId, 21, "Bob", "78.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitStatus").value(0));

        mockMvc.perform(post("/api/v2/evaluations/submit")
                        .header("X-Idempotency-Key", "idem-eval-submit-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":" + interviewId + ",\"interviewerId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitStatus").value(1));

        MvcResult summary = mockMvc.perform(get("/api/v2/evaluations/summary").param("interviewId", String.valueOf(interviewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalScore").exists())
                .andExpect(jsonPath("$.data.aiSuggestionOnly").value(true))
                .andReturn();
        JsonNode s = objectMapper.readTree(summary.getResponse().getContentAsString()).path("data");
        BigDecimal finalScore = new BigDecimal(s.path("finalScore").asText());
        assertEquals(0, finalScore.compareTo(new BigDecimal("80.00")));

        mockMvc.perform(post("/api/v2/evaluations/review")
                        .header("X-Idempotency-Key", "idem-eval-review-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":" + interviewId + ",\"interviewResult\":1,\"remark\":\"HR复核通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewResult").value(1))
                .andExpect(jsonPath("$.data.remark").value("HR复核通过"));

        mockMvc.perform(get("/api/v2/evaluations").param("interviewId", String.valueOf(interviewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluations").isArray())
                .andExpect(jsonPath("$.data.evaluations.length()").value(2))
                .andExpect(jsonPath("$.data.aiSuggestionOnly").value(true));
    }
}

