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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase2InterviewPlanTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createBody(long candidateId) {
        return """
                {
                  "candidateId": %d,
                  "applyPosition": "JAVA_ADV",
                  "interviewRound": "R1",
                  "interviewType": 1,
                  "templateId": 1,
                  "interviewStartTime": "2026-08-01T09:00:00",
                  "interviewEndTime": "2026-08-01T11:00:00",
                  "interviewRoomId": "room-a",
                  "interviewRoomLink": "https://meet.example/a",
                  "hrUserId": 10,
                  "interviewerIds": "20,21"
                }
                """.formatted(candidateId);
    }

    @Test
    void interviewPlanCrudAndStateMachine() throws Exception {
        String idemCreate = "phase2-ip-create-01";
        MvcResult created = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemCreate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(50001L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.candidateId").value(50001))
                .andExpect(jsonPath("$.data.applyPosition").value("JAVA_ADV"))
                .andExpect(jsonPath("$.data.interviewStatus").value(1))
                .andExpect(jsonPath("$.data.interviewCode").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(created.getResponse().getContentAsString());
        JsonNode data = root.path("data");
        long id = data.path("id").asLong();
        String code = data.path("interviewCode").asText();

        mockMvc.perform(get("/api/interview-plans/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.interviewCode").value(code));

        mockMvc.perform(get("/api/interview-plans/code/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        String updateJson = """
                {"applyPosition": "JAVA_SENIOR","remark": "更新备注"}
                """;
        mockMvc.perform(put("/api/interview-plans/" + id)
                        .header("X-Idempotency-Key", "phase2-ip-update-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applyPosition").value("JAVA_SENIOR"))
                .andExpect(jsonPath("$.data.remark").value("更新备注"));

        mockMvc.perform(post("/api/interview-plans/" + id + "/start")
                        .header("X-Idempotency-Key", "phase2-ip-start-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewStatus").value(2));

        mockMvc.perform(post("/api/interview-plans/" + id + "/complete")
                        .header("X-Idempotency-Key", "phase2-ip-complete-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewStatus").value(3));

        String idemCreate2 = "phase2-ip-create-02";
        MvcResult created2 = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemCreate2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(50002L)))
                .andExpect(status().isCreated())
                .andReturn();
        long id2 = objectMapper.readTree(created2.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/interview-plans/" + id2 + "/cancel")
                        .header("X-Idempotency-Key", "phase2-ip-cancel-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewStatus").value(4));

        String idemCreate3 = "phase2-ip-create-03";
        MvcResult created3 = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemCreate3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(50003L)))
                .andExpect(status().isCreated())
                .andReturn();
        long id3 = objectMapper.readTree(created3.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/interview-plans/" + id3 + "/complete")
                        .header("X-Idempotency-Key", "phase2-ip-bad-complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.errorCode").value("3001"));

        String idemCreate4 = "phase2-ip-create-04";
        MvcResult created4 = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemCreate4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(50004L)))
                .andExpect(status().isCreated())
                .andReturn();
        long id4 = objectMapper.readTree(created4.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/interview-plans/" + id4 + "/start")
                        .header("X-Idempotency-Key", "phase2-ip-start-04"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/interview-plans/" + id4)
                        .header("X-Idempotency-Key", "phase2-ip-update-deny")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"不应成功\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3002))
                .andExpect(jsonPath("$.errorCode").value("3002"));

        mockMvc.perform(get("/api/interview-plans").param("status", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        String idemSame = "phase2-ip-idem-same";
        MvcResult first = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemSame)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(60001L)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode d1 = objectMapper.readTree(first.getResponse().getContentAsString()).path("data");
        long idFirst = d1.path("id").asLong();
        String codeFirst = d1.path("interviewCode").asText();

        MvcResult second = mockMvc.perform(post("/api/interview-plans")
                        .header("X-Idempotency-Key", idemSame)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(60001L)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode d2 = objectMapper.readTree(second.getResponse().getContentAsString()).path("data");
        long idSecond = d2.path("id").asLong();
        String codeSecond = d2.path("interviewCode").asText();

        org.junit.jupiter.api.Assertions.assertEquals(idFirst, idSecond);
        org.junit.jupiter.api.Assertions.assertEquals(codeFirst, codeSecond);
    }
}
