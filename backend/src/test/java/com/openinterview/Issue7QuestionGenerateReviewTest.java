package com.openinterview;

import com.openinterview.service.InMemoryWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #7：题目生成 + 审核（HITL Slice-05）
 */
@SpringBootTest
@AutoConfigureMockMvc
class Issue7QuestionGenerateReviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryWorkflowService workflowService;

    @Test
    void a_generateQuestionsReturnsRequestCodeAndPayload() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-a-gen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":70001,\"resumeSectionId\":\"sec_proj_a\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.bizCode").exists())
                .andExpect(jsonPath("$.data.requestCode").exists())
                .andExpect(jsonPath("$.data.inputSnapshotHash").exists())
                .andExpect(jsonPath("$.data.reviewStatus").value(1))
                .andExpect(jsonPath("$.data.questionCount").value(3))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andExpect(jsonPath("$.data.questions[0].stem").exists());
    }

    @Test
    void b_reviewApproveFromPending() throws Exception {
        MvcResult gen = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-b-gen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":70002,\"resumeSectionId\":\"sec_b\",\"difficultyLevel\":1,\"questionCount\":2}"))
                .andExpect(status().isOk())
                .andReturn();
        String rc = extract(gen.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "issue7-b-rev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + rc + "\",\"reviewStatus\":2,\"reviewComment\":\"通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value(2))
                .andExpect(jsonPath("$.data.reviewComment").value("通过"))
                .andExpect(jsonPath("$.data.reviewTime").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.bizCode").exists());
    }

    @Test
    void c_reviewRejectFromPending() throws Exception {
        MvcResult gen = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-c-gen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":70003,\"resumeSectionId\":\"sec_c\",\"difficultyLevel\":3,\"questionCount\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        String rc = extract(gen.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "issue7-c-rev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + rc + "\",\"reviewStatus\":3,\"reviewComment\":\"题干与岗位不符\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value(3))
                .andExpect(jsonPath("$.data.reviewComment").value("题干与岗位不符"));
    }

    @Test
    void d_secondReviewRejectedWithStateMachine4009() throws Exception {
        MvcResult gen = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-d-gen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":70004,\"resumeSectionId\":\"sec_d\",\"difficultyLevel\":2,\"questionCount\":2}"))
                .andExpect(status().isOk())
                .andReturn();
        String rc = extract(gen.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "issue7-d-r1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + rc + "\",\"reviewStatus\":2,\"reviewComment\":\"ok\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "issue7-d-r2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + rc + "\",\"reviewStatus\":3,\"reviewComment\":\"again\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4009));
    }

    @Test
    void e_idempotentReplayMatches() throws Exception {
        String body = "{\"interviewId\":70005,\"resumeSectionId\":\"sec_e\",\"difficultyLevel\":2,\"questionCount\":2}";
        MvcResult first = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-e-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-e-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String b1 = first.getResponse().getContentAsString();
        String b2 = second.getResponse().getContentAsString();
        String rc1 = extract(b1, "\"requestCode\":\"", "\"");
        String rc2 = extract(b2, "\"requestCode\":\"", "\"");
        String h1 = extract(b1, "\"inputSnapshotHash\":\"", "\"");
        String h2 = extract(b2, "\"inputSnapshotHash\":\"", "\"");
        org.junit.jupiter.api.Assertions.assertEquals(rc1, rc2);
        org.junit.jupiter.api.Assertions.assertEquals(h1, h2);
    }

    @Test
    void f_missingRequiredFieldReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-f-miss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":70006,\"difficultyLevel\":2,\"questionCount\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void g_interviewNotFound() throws Exception {
        long missingId = 999_888_777L;
        workflowService.markInterviewMissing(missingId);
        mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "issue7-g-none")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":" + missingId + ",\"resumeSectionId\":\"sec_g\",\"difficultyLevel\":2,\"questionCount\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4007));
    }

    private static String extract(String body, String left, String right) {
        int i = body.indexOf(left);
        if (i < 0) {
            return "";
        }
        int s = i + left.length();
        int e = body.indexOf(right, s);
        if (e < 0) {
            return "";
        }
        return body.substring(s, e);
    }
}
