package com.openinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContractBaselineApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void writeApiMissingIdempotencyHeaderShouldFail() throws Exception {
        String body = """
                {"candidateId":10001,"jobCode":"JAVA_ADV_01"}
                """;
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void screenAndResultShouldContainEventMappingAndTrace() throws Exception {
        String body = """
                {"candidateId":10001,"jobCode":"JAVA_ADV_01"}
                """;
        mockMvc.perform(post("/api/v1/candidate/resume/screen")
                        .header("X-Trace-Id", "trace_case_01")
                        .header("X-Idempotency-Key", "idem-case-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace_case_01"))
                .andExpect(jsonPath("$.data.screenStatus").value(2))
                .andExpect(jsonPath("$.data.mqEventCode").value("candidate.resume.screen"))
                .andExpect(jsonPath("$.data.webhookEventCode").value("candidate.resume.screened"))
                .andExpect(jsonPath("$.traceId").value("trace_case_01"));

        mockMvc.perform(get("/api/v1/candidate/resume/screen/result/10001")
                        .param("jobCode", "JAVA_ADV_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendLevel").value(1));
    }

    @Test
    void exportTaskShouldUseUnifiedExportType() throws Exception {
        String body = """
                {"interviewIds":[50001]}
                """;
        String resp = mockMvc.perform(post("/api/v1/export/word")
                        .header("X-Idempotency-Key", "idem-export-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(2))
                .andReturn().getResponse().getContentAsString();

        String taskId = resp.replaceAll(".*\"taskId\":([0-9]+).*", "$1");
        mockMvc.perform(get("/api/v1/export/task/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(2));
    }
}
