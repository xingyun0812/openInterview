package com.openinterview;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.export.ScoreExcelRow;
import com.openinterview.service.InMemoryWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase2RealExportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Path exportStorageDir;

    static {
        try {
            exportStorageDir = Files.createTempDirectory("openinterview-v2-export");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void registerExportDir(DynamicPropertyRegistry registry) {
        registry.add("export.storage-dir", () -> exportStorageDir.toAbsolutePath().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void screeningExcelIsRealXlsx() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v2/exports")
                        .header("X-Idempotency-Key", "idem-v2-screen-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":0,\"exportContent\":\"101,102\",\"jobCode\":\"JAVA_ADV\","
                                + "\"exportUserId\":1,\"exportUserName\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(InMemoryWorkflowService.TASK_SUCCESS))
                .andExpect(jsonPath("$.data.exportType").value(0))
                .andReturn();
        long taskId = MAPPER.readTree(res.getResponse().getContentAsString()).path("data").path("taskId").asLong();
        MvcResult dl = mockMvc.perform(get("/api/v2/exports/" + taskId + "/download"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] file = dl.getResponse().getContentAsByteArray();
        assertTrue(isZipMagic(file), "应为 ZIP/Office Open XML（PK 头）");
        assertTrue(dl.getResponse().getContentType() != null
                && dl.getResponse().getContentType().contains("spreadsheetml"));
    }

    @Test
    void scoreExcelCanBeReadBackWithEasyExcel() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v2/exports")
                        .header("X-Idempotency-Key", "idem-v2-score-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"exportContent\":\"88001,88002\","
                                + "\"exportUserId\":1,\"exportUserName\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(InMemoryWorkflowService.TASK_SUCCESS))
                .andReturn();
        long taskId = MAPPER.readTree(res.getResponse().getContentAsString()).path("data").path("taskId").asLong();
        MvcResult dl = mockMvc.perform(get("/api/v2/exports/" + taskId + "/download"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] file = dl.getResponse().getContentAsByteArray();
        List<ScoreExcelRow> rows;
        try (ByteArrayInputStream in = new ByteArrayInputStream(file)) {
            rows = EasyExcel.read(in).head(ScoreExcelRow.class).sheet().doReadSync();
        }
        assertEquals(2, rows.size());
        assertEquals(88001L, rows.get(0).getInterviewId());
        assertNotNull(rows.get(0).getFinalScore());
    }

    @Test
    void interviewWordIsRealDocx() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v2/exports")
                        .header("X-Idempotency-Key", "idem-v2-word-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":2,\"exportContent\":\"99001\","
                                + "\"exportUserId\":1,\"exportUserName\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportType").value(2))
                .andReturn();
        long taskId = MAPPER.readTree(res.getResponse().getContentAsString()).path("data").path("taskId").asLong();
        MvcResult dl = mockMvc.perform(get("/api/v2/exports/" + taskId + "/download"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] file = dl.getResponse().getContentAsByteArray();
        assertTrue(isZipMagic(file), "docx 应为 ZIP（PK 头）");
        assertTrue(dl.getResponse().getContentType() != null
                && dl.getResponse().getContentType().contains("wordprocessingml"));
    }

    @Test
    void invalidExportTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/v2/exports")
                        .header("X-Idempotency-Key", "idem-v2-invalid-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":5,\"exportContent\":\"1\","
                                + "\"exportUserId\":1,\"exportUserName\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadReturnsBytes() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v2/exports")
                        .header("X-Idempotency-Key", "idem-v2-dl-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportType\":1,\"exportContent\":\"77001\","
                                + "\"exportUserId\":1,\"exportUserName\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = MAPPER.readTree(res.getResponse().getContentAsString()).path("data").path("taskId").asLong();
        mockMvc.perform(get("/api/v2/exports/" + taskId + "/download"))
                .andExpect(status().isOk())
                .andExpect(r -> assertTrue(r.getResponse().getContentAsByteArray().length > 100));
    }

    private static boolean isZipMagic(byte[] file) {
        return file != null && file.length >= 4 && file[0] == 'P' && file[1] == 'K';
    }

}
