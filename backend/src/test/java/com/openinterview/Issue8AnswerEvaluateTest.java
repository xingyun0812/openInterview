package com.openinterview;

import com.openinterview.service.InMemoryWorkflowService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Issue8AnswerEvaluateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryWorkflowService workflowService;

    @Test
    void a_normalEvaluateReturnsThreeScoresAndFollowUp() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88001,\"questionId\":99001,\"answerText\":\"候选人回答了分布式事务与一致性\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accuracyScore").value(82.5))
                .andExpect(jsonPath("$.data.coverageScore").value(78))
                .andExpect(jsonPath("$.data.clarityScore").value(80))
                .andExpect(jsonPath("$.data.followUpSuggest").exists());
    }

    @Test
    void b_blankAnswerRejectedWith4012() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88002,\"questionId\":99002,\"answerText\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4012))
                .andExpect(jsonPath("$.errorCode").value("4012"));
    }

    @Test
    void c_idempotentReplayReturnsSamePayload() throws Exception {
        String body = "{\"interviewId\":88003,\"questionId\":99003,\"answerText\":\"幂等测试答案\"}";
        MvcResult first = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        String s1 = first.getResponse().getContentAsString();
        String s2 = second.getResponse().getContentAsString();
        Assertions.assertEquals(extract(s1, "\"bizCode\":\"", "\""), extract(s2, "\"bizCode\":\"", "\""));
        Assertions.assertEquals(extract(s1, "\"accuracyScore\":", ","), extract(s2, "\"accuracyScore\":", ","));
    }

    @Test
    void d_traceIdAndBizCodePresent() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-d")
                        .header("X-Trace-Id", "trace-issue8-d")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88004,\"questionId\":99004,\"answerText\":\"trace 测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("trace-issue8-d"))
                .andExpect(jsonPath("$.bizCode").exists());
    }

    @Test
    void e_aiSuggestionOnlyFlagTrue() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-e")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88005,\"questionId\":99005,\"answerText\":\"标记测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiSuggestionOnly").value(true));
    }

    @Test
    void f_eventPublishedVisibleInEvidence() throws Exception {
        MvcResult eval = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-f")
                        .header("X-Trace-Id", "trace-issue8-f")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88006,\"questionId\":99006,\"answerText\":\"事件测试\"}"))
                .andExpect(status().isOk()).andReturn();
        String bizCode = extract(eval.getResponse().getContentAsString(), "\"bizCode\":\"", "\"");

        MvcResult ev = mockMvc.perform(get("/api/v1/internal/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mqEvents").isArray())
                .andReturn();
        String evidence = ev.getResponse().getContentAsString();
        Assertions.assertTrue(evidence.contains("\"eventCode\":\"interview.answer.evaluate\""));
        Assertions.assertTrue(evidence.contains("\"bizCode\":\"" + bizCode + "\""));
        Assertions.assertTrue(evidence.contains("trace-issue8-f"));
    }

    @Test
    void g_auditTrailRecordsEvaluate() throws Exception {
        MvcResult eval = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-g")
                        .header("X-Trace-Id", "trace-issue8-g")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88007,\"questionId\":99007,\"answerText\":\"审计测试\"}"))
                .andExpect(status().isOk()).andReturn();
        String bizCode = extract(eval.getResponse().getContentAsString(), "\"bizCode\":\"", "\"");

        MvcResult ev = mockMvc.perform(get("/api/v1/internal/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditLogs").isArray())
                .andReturn();
        String evidence = ev.getResponse().getContentAsString();
        Assertions.assertTrue(evidence.contains("\"module\":\"interview\""));
        Assertions.assertTrue(evidence.contains("\"action\":\"assistant.answer.evaluate\""));
        Assertions.assertTrue(evidence.contains("\"bizCode\":\"" + bizCode + "\""));
        Assertions.assertTrue(evidence.contains("trace-issue8-g"));
    }

    @Test
    void assessRecordPersistedInWorkflowService() throws Exception {
        MvcResult eval = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "issue8-h")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":88008,\"questionId\":99008,\"answerText\":\"持久化测试\"}"))
                .andExpect(status().isOk()).andReturn();
        String bizCode = extract(eval.getResponse().getContentAsString(), "\"bizCode\":\"", "\"");
        List<InMemoryWorkflowService.AnswerAssessRecord> records = workflowService.getAnswerAssessRecords();
        boolean found = records.stream().anyMatch(r -> bizCode.equals(r.recordCode)
                && Long.valueOf(88008L).equals(r.interviewId)
                && Long.valueOf(99008L).equals(r.questionId));
        Assertions.assertTrue(found, "评估记录应写入 InMemoryWorkflowService");
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
