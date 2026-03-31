package com.openinterview.openapi;

import com.openinterview.entity.OpenApiAppEntity;
import com.openinterview.mapper.OpenApiAppMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.openinterview.openapi.OpenApiTestSupport.jsonBody;
import static com.openinterview.openapi.OpenApiTestSupport.jsonUtf8;
import static com.openinterview.openapi.OpenApiTestSupport.nowEpochSeconds;
import static com.openinterview.openapi.OpenApiTestSupport.sign;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase2OpenApiSignTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    OpenApiAppMapper openApiAppMapper;

    private String appId;
    private String appSecret;

    @BeforeEach
    void setUp() {
        appId = "app-" + UUID.randomUUID().toString().substring(0, 8);
        appSecret = "sec-" + UUID.randomUUID().toString().replace("-", "");

        OpenApiAppEntity app = new OpenApiAppEntity();
        app.appId = appId;
        app.appSecret = appSecret;
        app.appName = "test";
        app.appDesc = "test";
        app.apiPermissions = "";
        app.status = 1;
        app.createUser = 0L;
        app.createTime = LocalDateTime.now();
        app.updateTime = LocalDateTime.now();
        app.isDeleted = 0;
        openApiAppMapper.insert(app);
    }

    @Test
    void correctSignature_shouldReturn200() throws Exception {
        byte[] body = jsonBody(Map.of(
                "name", "张三",
                "phone", "13800000000",
                "applyPosition", "Java"
        ));
        long ts = nowEpochSeconds();
        String nonce = "n1";

        var req = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body);
        req = sign(req, appId, appSecret, "POST", "/open-api/candidates/push", null,
                MediaType.APPLICATION_JSON_VALUE, body, ts, nonce);

        mockMvc.perform(req).andExpect(status().isOk());
    }

    @Test
    void wrongSignature_shouldReturn401() throws Exception {
        byte[] body = jsonBody(Map.of(
                "name", "张三",
                "phone", "13800000000",
                "applyPosition", "Java"
        ));
        long ts = nowEpochSeconds();
        String nonce = "n2";

        var req = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body)
                .header(OpenApiConstants.HEADER_APP_ID, appId)
                .header(OpenApiConstants.HEADER_TIMESTAMP, String.valueOf(ts))
                .header(OpenApiConstants.HEADER_NONCE, nonce)
                .header(OpenApiConstants.HEADER_SIGNATURE, "deadbeef");

        mockMvc.perform(req).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTimestamp_shouldReturn401() throws Exception {
        byte[] body = jsonBody(Map.of(
                "name", "张三",
                "phone", "13800000000",
                "applyPosition", "Java"
        ));
        long ts = nowEpochSeconds() - 1000;
        String nonce = "n3";

        var req = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body);
        req = sign(req, appId, appSecret, "POST", "/open-api/candidates/push", null,
                MediaType.APPLICATION_JSON_VALUE, body, ts, nonce);

        mockMvc.perform(req).andExpect(status().isUnauthorized());
    }

    @Test
    void nonceReplay_shouldReturn401() throws Exception {
        byte[] body = jsonBody(Map.of(
                "name", "张三",
                "phone", "13800000000",
                "applyPosition", "Java"
        ));
        long ts = nowEpochSeconds();
        String nonce = "n4";

        var req1 = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body);
        req1 = sign(req1, appId, appSecret, "POST", "/open-api/candidates/push", null,
                MediaType.APPLICATION_JSON_VALUE, body, ts, nonce);
        mockMvc.perform(req1).andExpect(status().isOk());

        var req2 = post("/open-api/candidates/push")
                .contentType(jsonUtf8())
                .content(body);
        req2 = sign(req2, appId, appSecret, "POST", "/open-api/candidates/push", null,
                MediaType.APPLICATION_JSON_VALUE, body, ts, nonce);
        mockMvc.perform(req2).andExpect(status().isUnauthorized());
    }
}

