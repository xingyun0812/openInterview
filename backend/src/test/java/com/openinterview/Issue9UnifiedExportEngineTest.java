package com.openinterview;

import com.openinterview.service.AuditTrailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Issue9UnifiedExportEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditTrailService auditTrailService;

    @Test
    void scoreExcelExportUnifiedTaskShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-score-ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[88001,88002]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(1))
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.fileHash").isString())
                .andExpect(jsonPath("$.data.fileSize").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void interviewWordExportUnifiedTaskShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-word-ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":2,\"interviewIds\":[89001]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(2))
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.fileName").value(org.hamcrest.Matchers.containsString("interview-")))
                .andExpect(jsonPath("$.data.fileSize").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void failedTaskFirstRetryShouldSucceed() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-retry-ok-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[910001]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-retry-ok-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.retryCount").value(1))
                .andExpect(jsonPath("$.data.fileHash").exists());

        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.fileSize").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void retriesExhaustedShouldEnterDlq() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-dlq-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[910002]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-dlq-r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andExpect(jsonPath("$.data.retryCount").value(1));

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-dlq-r2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryCount").value(2));

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-dlq-r3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryCount").value(3));

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-dlq-r4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andExpect(jsonPath("$.data.failReason").value("重试次数已耗尽"))
                .andExpect(jsonPath("$.data.retryCount").value(4));

        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failReason").value("重试次数已耗尽"));
    }

    @Test
    void fileMetadataShouldHaveSha256AndPositiveSize() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-meta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":2,\"interviewIds\":[89002]}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = create.getResponse().getContentAsString();
        String hash = extract(body, "\"fileHash\":\"", "\"");
        assertEquals(64, hash.length());
        mockMvc.perform(get("/api/v1/export/task/" + extract(body, "\"taskId\":", ",")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileSize").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.fileHash").isString());
    }

    @Test
    void idempotentReplayShouldReturnSameTask() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[88010]}"))
                .andExpect(status().isOk()).andReturn();
        MvcResult second = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-idem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[88010]}"))
                .andExpect(status().isOk()).andReturn();
        String b1 = first.getResponse().getContentAsString();
        String b2 = second.getResponse().getContentAsString();
        assertEquals(extract(b1, "\"taskId\":", ","), extract(b2, "\"taskId\":", ","));
        assertEquals(extract(b1, "\"taskCode\":\"", "\""), extract(b2, "\"taskCode\":\"", "\""));
    }

    @Test
    void nonFailedTaskRetryShouldReject() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-noretry-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[88020]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue9-noretry-b"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void unknownTaskIdQueryShould404() throws Exception {
        mockMvc.perform(get("/api/v1/export/task/999999992"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(6001));
    }

    @Test
    void auditTrailShouldRecordExportOperations() throws Exception {
        int before = auditTrailService.list().size();
        mockMvc.perform(post("/api/v1/export/task")
                        .header("X-Idempotency-Key", "issue9-audit-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"interviewIds\":[88030]}"))
                .andExpect(status().isOk());
        assertTrue(auditTrailService.list().size() > before);
        assertTrue(auditTrailService.list().stream().anyMatch(r -> "export.create".equals(r.action)));
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
