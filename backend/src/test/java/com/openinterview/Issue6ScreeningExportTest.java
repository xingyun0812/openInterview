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
class Issue6ScreeningExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void screeningExportShouldReturnType0AndCoreTaskFields() throws Exception {
        mockMvc.perform(post("/api/v1/export/screening/excel")
                        .header("X-Idempotency-Key", "issue6-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[6001,6002],\"jobCode\":\"JAVA_ADV_01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(0))
                .andExpect(jsonPath("$.data.exportTypeLabel").value("筛选Excel"))
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.fileHash").exists());
    }

    @Test
    void taskStatusShouldContainHashAndStateFlow() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/screening/excel")
                        .header("X-Idempotency-Key", "issue6-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[6003],\"jobCode\":\"JAVA_ADV_02\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileHash").exists())
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andExpect(jsonPath("$.data.stateFlow").isArray());
    }

    @Test
    void failedTaskShouldBeRetryableAndTrackRetryCount() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/screening/excel")
                        .header("X-Idempotency-Key", "issue6-03a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[6004],\"jobCode\":\"FAIL_EXPORT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andExpect(jsonPath("$.data.failReason").exists())
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue6-03b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryCount").value(1));

        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(3))
                .andExpect(jsonPath("$.data.retryCount").value(1));
    }

    @Test
    void successTaskShouldRejectRetry() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/export/screening/excel")
                        .header("X-Idempotency-Key", "issue6-04a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[6005],\"jobCode\":\"JAVA_ADV_03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(2))
                .andReturn();
        String taskId = extract(create.getResponse().getContentAsString(), "\"taskId\":", ",");

        mockMvc.perform(post("/api/v1/export/task/" + taskId + "/retry")
                        .header("X-Idempotency-Key", "issue6-04b"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
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
