package com.openinterview.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.entity.OpenApiAppEntity;
import com.openinterview.mapper.OpenApiAppMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.openinterview.openapi.OpenApiTestSupport.jsonBody;
import static com.openinterview.openapi.OpenApiTestSupport.jsonUtf8;
import static com.openinterview.openapi.OpenApiTestSupport.nowEpochSeconds;
import static com.openinterview.openapi.OpenApiTestSupport.sign;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase2OpenApiFlowTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    OpenApiAppMapper openApiAppMapper;

    private final ObjectMapper mapper = new ObjectMapper();
    private String appId;
    private String appSecret;

    @BeforeEach
    void setUp() {
        appId = "app-" + UUID.randomUUID().toString().substring(0, 8);
        appSecret = "sec-" + UUID.randomUUID().toString().replace("-", "");

        OpenApiAppEntity app = new OpenApiAppEntity();
        app.appId = appId;
        app.appSecret = appSecret;
        app.appName = "test-flow";
        app.appDesc = "test-flow";
        app.apiPermissions = "candidates:push,interviews:result:read,webhooks:subscribe";
        app.status = 1;
        app.createUser = 0L;
        app.createTime = LocalDateTime.now();
        app.updateTime = LocalDateTime.now();
        app.isDeleted = 0;
        openApiAppMapper.insert(app);
    }

    @Test
    void pushCandidate_thenQueryInterviewResult_mocked() throws Exception {
        // push candidate
        byte[] body = jsonBody(Map.of(
                "name", "李四",
                "phone", "13900000000",
                "applyPosition", "Java"
        ));
        long ts = nowEpochSeconds();
        String nonce = "flow1";

        var pushReq = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body);
        pushReq = sign(pushReq, appId, appSecret, "POST", "/open-api/candidates/push", null,
                MediaType.APPLICATION_JSON_VALUE, body, ts, nonce);

        MvcResult pushRes = mockMvc.perform(pushReq)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode pushNode = mapper.readTree(pushRes.getResponse().getContentAsByteArray());
        assertThat(pushNode.path("data").path("candidateCode").asText()).isNotBlank();

        // query interview result with a fake code（目前允许返回 400，最小可用）
        String interviewCode = "INT_FAKE_CODE";
        long ts2 = nowEpochSeconds();
        String nonce2 = "flow2";

        var getReq = get("/open-api/interviews/" + interviewCode + "/result");
        getReq = sign(getReq, appId, appSecret, "GET",
                "/open-api/interviews/" + interviewCode + "/result", null,
                null, null, ts2, nonce2);

        mockMvc.perform(getReq).andExpect(status().isBadRequest());
    }
}

