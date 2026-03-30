package com.openinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Regression30ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void case01_screenMissingIdemHeaderShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10001,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case02_screenMissingCandidateShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case03_screenSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-03")
                        .header("X-Trace-Id", "trace-r30-03")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10003,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.screenStatus").value(2))
                .andExpect(jsonPath("$.traceId").value("trace-r30-03"));
    }

    @Test
    void case04_screenIdempotentRepeatShouldReturnSameTaskCode() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10004,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-04")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10004,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk()).andReturn();
        String task1 = extract(first.getResponse().getContentAsString(), "\"taskCode\":\"", "\"");
        String task2 = extract(second.getResponse().getContentAsString(), "\"taskCode\":\"", "\"");
        org.junit.jupiter.api.Assertions.assertEquals(task1, task2);
    }

    @Test
    void case05_screenResultShouldSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-05")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10005,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/candidate/resume/screen/result/10005")
                        .param("jobCode", "JAVA_ADV_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.screenStatus").value(2))
                .andExpect(jsonPath("$.data.recommendLevel").value(1));
    }

    @Test
    void case06_screenResultNotFoundShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/candidate/resume/screen/result/19999")
                        .param("jobCode", "JAVA_ADV_01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(8002));
    }

    @Test
    void case07_screenReviewSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-07a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10007,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "r30-07b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10007,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":1,\"reviewComment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewResult").value(1));
    }

    @Test
    void case08_screenReviewIdempotentRepeat() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-08a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10008,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk());
        MvcResult first = mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "r30-08b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10008,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":2,\"reviewComment\":\"pending\"}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "r30-08b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10008,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":2,\"reviewComment\":\"pending\"}"))
                .andExpect(status().isOk()).andReturn();
        String body1 = first.getResponse().getContentAsString();
        String body2 = second.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"bizCode\":\"", "\""),
                extract(body2, "\"bizCode\":\"", "\"")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"reviewResult\":", ","),
                extract(body2, "\"reviewResult\":", ",")
        );
    }

    @Test
    void case09_screenReviewMissingCommentFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen/review")
                        .header("X-Idempotency-Key", "r30-09")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10009,\"jobCode\":\"JAVA_ADV_01\",\"reviewResult\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case10_screenResponseShouldContainEventMapping() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Idempotency-Key", "r30-10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":10010,\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mqEventCode").value("candidate.resume.screen"))
                .andExpect(jsonPath("$.data.webhookEventCode").value("candidate.resume.screened"));
    }

    @Test
    void case11_questionGenerateSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50011,\"resumeSectionId\":\"project_1\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value(1));
    }

    @Test
    void case12_questionGenerateMissingFieldFail() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50012,\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case13_questionGenerateIdempotentRepeat() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-13")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50013,\"resumeSectionId\":\"project_1\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-13")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50013,\"resumeSectionId\":\"project_1\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk()).andReturn();
        String req1 = extract(first.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        String req2 = extract(second.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        org.junit.jupiter.api.Assertions.assertEquals(req1, req2);
    }

    @Test
    void case14_questionReviewSuccess() throws Exception {
        MvcResult generate = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-14a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50014,\"resumeSectionId\":\"project_1\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk()).andReturn();
        String requestCode = extract(generate.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "r30-14b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + requestCode + "\",\"reviewStatus\":2,\"reviewComment\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value(2));
    }

    @Test
    void case15_questionReviewIllegalTransitionFail() throws Exception {
        MvcResult generate = mockMvc.perform(post("/api/v1/interview/assistant/question/generate")
                        .header("X-Idempotency-Key", "r30-15a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50015,\"resumeSectionId\":\"project_1\",\"difficultyLevel\":2,\"questionCount\":3}"))
                .andExpect(status().isOk()).andReturn();
        String requestCode = extract(generate.getResponse().getContentAsString(), "\"requestCode\":\"", "\"");
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "r30-15b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + requestCode + "\",\"reviewStatus\":2,\"reviewComment\":\"pass\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "r30-15c")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"" + requestCode + "\",\"reviewStatus\":3,\"reviewComment\":\"reject\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(8004));
    }

    @Test
    void case16_questionReviewNotFoundFail() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/question/review")
                        .header("X-Idempotency-Key", "r30-16")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestCode\":\"NONE\",\"reviewStatus\":2,\"reviewComment\":\"pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case17_answerEvaluateSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "r30-17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50017,\"questionId\":70017,\"answerText\":\"answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accuracyScore").exists())
                .andExpect(jsonPath("$.data.webhookEventCode").value("interview.answer.evaluated"));
    }

    @Test
    void case18_answerEvaluateEmptyAnswerFail() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "r30-18")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50018,\"questionId\":70018,\"answerText\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case19_answerEvaluateIdempotentRepeat() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "r30-19")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50019,\"questionId\":70019,\"answerText\":\"answer\"}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .header("X-Idempotency-Key", "r30-19")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50019,\"questionId\":70019,\"answerText\":\"answer\"}"))
                .andExpect(status().isOk()).andReturn();
        String body1 = first.getResponse().getContentAsString();
        String body2 = second.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"bizCode\":\"", "\""),
                extract(body2, "\"bizCode\":\"", "\"")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"accuracyScore\":", ","),
                extract(body2, "\"accuracyScore\":", ",")
        );
    }

    @Test
    void case20_answerEvaluateMissingIdemFail() throws Exception {
        mockMvc.perform(post("/api/v1/interview/assistant/answer/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewId\":50020,\"questionId\":70020,\"answerText\":\"answer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case21_exportScreeningSuccessType0() throws Exception {
        mockMvc.perform(post("/api/v1/export/screening/excel")
                        .header("X-Idempotency-Key", "r30-21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[10021],\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(0));
    }

    @Test
    void case22_exportExcelSuccessType1() throws Exception {
        mockMvc.perform(post("/api/v1/export/excel")
                        .header("X-Idempotency-Key", "r30-22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50022]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(1));
    }

    @Test
    void case23_exportWordSuccessType2() throws Exception {
        mockMvc.perform(post("/api/v1/export/word")
                        .header("X-Idempotency-Key", "r30-23")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50023]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(2));
    }

    @Test
    void case24_exportTaskQuerySuccess() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/word")
                        .header("X-Idempotency-Key", "r30-24")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50024]}"))
                .andExpect(status().isOk()).andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");
        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(Long.parseLong(taskId)));
    }

    @Test
    void case25_exportTaskNotFoundFail() throws Exception {
        mockMvc.perform(get("/api/v1/export/task/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(6001));
    }

    @Test
    void case26_exportIdempotentRepeat() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/export/excel")
                        .header("X-Idempotency-Key", "r30-26")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50026]}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/export/excel")
                        .header("X-Idempotency-Key", "r30-26")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50026]}"))
                .andExpect(status().isOk()).andReturn();
        String body1 = first.getResponse().getContentAsString();
        String body2 = second.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"taskCode\":\"", "\""),
                extract(body2, "\"taskCode\":\"", "\"")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                extract(body1, "\"taskId\":", ","),
                extract(body2, "\"taskId\":", ",")
        );
    }

    @Test
    void case27_exportMissingInterviewIdsFail() throws Exception {
        mockMvc.perform(post("/api/v1/export/excel")
                        .header("X-Idempotency-Key", "r30-27")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case28_eventsEvidenceShouldContainMqEvents() throws Exception {
        mockMvc.perform(post("/api/v1/export/word")
                        .header("X-Idempotency-Key", "r30-28")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50028]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/internal/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mqEvents").isArray());
    }

    @Test
    void case29_traceBizErrorFieldsShouldExistInSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/v1/export/word")
                        .header("X-Idempotency-Key", "r30-29")
                        .header("X-Trace-Id", "trace-r30-29")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interviewIds\":[50029]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("trace-r30-29"))
                .andExpect(jsonPath("$.bizCode").exists());
    }

    @Test
    void case30_traceBizErrorFieldsShouldExistInFailureResponse() throws Exception {
        mockMvc.perform(get("/api/v1/export/task/999999991"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.bizCode").exists())
                .andExpect(jsonPath("$.errorCode").exists());
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
