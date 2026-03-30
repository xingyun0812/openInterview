package com.openinterview;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Issue4ResumeParseLoopTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void case01_uploadAndParseSuccess() throws Exception {
        String resumeUrl = uploadResume(40001, "ok-resume.pdf", "java spring mysql");
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Trace-Id", "trace-i4-01")
                        .header("X-Idempotency-Key", "i4-01-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40001,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parseStatus").value(1))
                .andExpect(jsonPath("$.traceId").value("trace-i4-01"));

        waitParseStatus(40001, 2, 2000);
    }

    @Test
    void case02_parseMissingCandidateIdShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-02-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeUrl\":\"mock://resume/1/a.pdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case03_parseIdempotentRepeatShouldReturnSameTaskCode() throws Exception {
        String resumeUrl = uploadResume(40003, "idem-resume.pdf", "idem");
        MvcResult first = mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-03-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40003,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-03-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40003,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk()).andReturn();

        String taskCode1 = extract(first.getResponse().getContentAsString(), "\"taskCode\":\"", "\"");
        String taskCode2 = extract(second.getResponse().getContentAsString(), "\"taskCode\":\"", "\"");
        Assertions.assertEquals(taskCode1, taskCode2);
    }

    @Test
    void case04_parseFailureShouldRetryAndAudit() throws Exception {
        String resumeUrl = uploadResume(40004, "fail-always-resume.pdf", "fail");
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Trace-Id", "trace-i4-04")
                        .header("X-Idempotency-Key", "i4-04-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40004,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk());

        waitParseStatus(40004, 3, 5000);

        MvcResult auditResult = mockMvc.perform(get("/api/v1/internal/evidence/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parseFailureAudits").isArray())
                .andReturn();
        String auditBody = auditResult.getResponse().getContentAsString();
        Assertions.assertTrue(auditBody.contains("trace-i4-04"));
        Assertions.assertTrue(auditBody.contains("\"errorCode\":\"8001\""));
    }

    @Test
    void case05_parseResultShouldContainStructuredFields() throws Exception {
        String resumeUrl = uploadResume(40005, "structure-resume.pdf", "java");
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-05-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40005,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk());

        waitParseStatus(40005, 2, 2000);
        mockMvc.perform(get("/api/v1/candidate/resume/parse/result/40005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basicInfo.name").exists())
                .andExpect(jsonPath("$.data.education").isArray())
                .andExpect(jsonPath("$.data.workExperience").isArray())
                .andExpect(jsonPath("$.data.skillTags").isArray());
    }

    @Test
    void case06_uploadMissingFileShouldFail() throws Exception {
        mockMvc.perform(multipart("/api/v1/candidate/resume/upload")
                        .param("candidateId", "40006")
                        .header("X-Idempotency-Key", "i4-06-upload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void case07_parseWithoutUploadShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-07-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40007,\"resumeUrl\":\"mock://resume/none/not-exist.pdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(8001));
    }

    @Test
    void case08_uploadIdempotentRepeatShouldReturnSameResumeUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("resumeFile", "same.pdf", "application/pdf", "content".getBytes());
        MvcResult first = mockMvc.perform(multipart("/api/v1/candidate/resume/upload")
                        .file(file)
                        .param("candidateId", "40008")
                        .header("X-Idempotency-Key", "i4-08-upload"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(multipart("/api/v1/candidate/resume/upload")
                        .file(file)
                        .param("candidateId", "40008")
                        .header("X-Idempotency-Key", "i4-08-upload"))
                .andExpect(status().isOk()).andReturn();
        String url1 = extract(first.getResponse().getContentAsString(), "\"resumeUrl\":\"", "\"");
        String url2 = extract(second.getResponse().getContentAsString(), "\"resumeUrl\":\"", "\"");
        Assertions.assertEquals(url1, url2);
    }

    @Test
    void case09_parseResponseShouldContainTraceBiz() throws Exception {
        String resumeUrl = uploadResume(40009, "trace-resume.pdf", "trace");
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Trace-Id", "trace-i4-09")
                        .header("X-Idempotency-Key", "i4-09-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40009,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value("trace-i4-09"))
                .andExpect(jsonPath("$.bizCode").exists());
    }

    @Test
    void case10_parseFailureResultShouldContainErrorCode() throws Exception {
        String resumeUrl = uploadResume(40010, "fail-always-second.pdf", "fail");
        mockMvc.perform(post("/api/v1/candidate/resume/parse")
                        .header("X-Idempotency-Key", "i4-10-parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":40010,\"resumeUrl\":\"" + resumeUrl + "\"}"))
                .andExpect(status().isOk());
        waitParseStatus(40010, 3, 5000);
        mockMvc.perform(get("/api/v1/candidate/resume/parse/result/40010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parseStatus").value(3))
                .andExpect(jsonPath("$.data.errorCode").value("8001"));
    }

    private String uploadResume(long candidateId, String fileName, String text) throws Exception {
        MockMultipartFile file = new MockMultipartFile("resumeFile", fileName, "application/pdf", text.getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/v1/candidate/resume/upload")
                        .file(file)
                        .param("candidateId", String.valueOf(candidateId))
                        .header("X-Idempotency-Key", "upload-" + candidateId))
                .andExpect(status().isOk())
                .andReturn();
        return extract(result.getResponse().getContentAsString(), "\"resumeUrl\":\"", "\"");
    }

    private void waitParseStatus(long candidateId, int expectedStatus, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            MvcResult result = mockMvc.perform(get("/api/v1/candidate/resume/parse/result/" + candidateId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            String statusText = extract(body, "\"parseStatus\":", ",");
            if (String.valueOf(expectedStatus).equals(statusText)) {
                return;
            }
            Thread.sleep(100L);
        }
        Assertions.fail("parse status not reached: " + expectedStatus);
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
