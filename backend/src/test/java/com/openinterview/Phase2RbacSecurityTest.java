package com.openinterview;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.entity.SysRoleEntity;
import com.openinterview.entity.SysUserEntity;
import com.openinterview.mapper.SysRoleMapper;
import com.openinterview.mapper.SysUserMapper;
import com.openinterview.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "openinterview.security.permit-all=false")
@AutoConfigureMockMvc
class Phase2RbacSecurityTest {

    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void seedUsers() {
        ensureUser("rbac_hr", PASSWORD, "HR");
        ensureUser("rbac_interviewer", PASSWORD, "INTERVIEWER");
        ensureUser("rbac_admin", PASSWORD, "ADMIN");
    }

    private void ensureUser(String username, String rawPassword, String roleCode) {
        if (sysUserMapper.selectOne(new QueryWrapper<SysUserEntity>().eq("username", username))
                != null) {
            return;
        }
        SysRoleEntity role = sysRoleMapper.selectOne(
                new QueryWrapper<SysRoleEntity>().eq("role_code", roleCode));
        assertNotNull(role, "role " + roleCode + " must exist (schema seed)");
        SysUserEntity u = new SysUserEntity();
        u.username = username;
        u.password = passwordEncoder.encode(rawPassword);
        u.realName = username;
        u.roleId = role.id;
        u.status = 1;
        sysUserMapper.insert(u);
    }

    @Test
    void unauthenticatedProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void loginReturns200AndTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rbac_hr\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void authenticatedRequestWithBearerReturns200() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rbac_hr\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/evidence/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void invalidBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/events")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLoginFlow() throws Exception {
        String u = "rbac_reg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u
                                + "\",\"password\":\"secret12\",\"realName\":\"新用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void refreshTokenIssuesNewAccessToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rbac_interviewer\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(login.getResponse().getContentAsString());
        String refresh = root.path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void jwtRoleClaimsMatchHrInterviewerAdmin() throws Exception {
        assertRole("rbac_hr", "HR");
        assertRole("rbac_interviewer", "INTERVIEWER");
        assertRole("rbac_admin", "ADMIN");
    }

    private void assertRole(String username, String expectedRole) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(expectedRole, jwtTokenProvider.getRoleFromToken(token));
    }
}
