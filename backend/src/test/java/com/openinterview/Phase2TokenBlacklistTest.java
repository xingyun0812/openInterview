package com.openinterview;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.entity.SysRoleEntity;
import com.openinterview.entity.SysUserEntity;
import com.openinterview.mapper.SysRoleMapper;
import com.openinterview.mapper.SysUserMapper;
import com.openinterview.security.JwtTokenProvider;
import com.openinterview.service.TokenBlacklistService;
import com.openinterview.util.CryptoHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "openinterview.security.permit-all=false",
        "openinterview.redis.enabled=true"
})
@AutoConfigureMockMvc
class Phase2TokenBlacklistTest {

    private static final String PASSWORD = "password";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

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
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void seedUser() {
        ensureUser("blacklist_hr", PASSWORD, "HR");
    }

    private void ensureUser(String username, String rawPassword, String roleCode) {
        if (sysUserMapper.selectOne(new QueryWrapper<SysUserEntity>().eq("username", username)) != null) {
            return;
        }
        SysRoleEntity role = sysRoleMapper.selectOne(new QueryWrapper<SysRoleEntity>().eq("role_code", roleCode));
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
    void blacklistedTokenShouldReturn401() throws Exception {
        stringRedisTemplate.opsForValue().set("test:redis:ping", "1", Duration.ofSeconds(5));
        assertTrue("1".equals(stringRedisTemplate.opsForValue().get("test:redis:ping")));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"blacklist_hr\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();

        Date exp = jwtTokenProvider.getExpirationFromToken(token);
        long ttlMs = Math.max(1, exp.getTime() - System.currentTimeMillis());
        String tokenId = CryptoHash.sha256Hex(token);
        tokenBlacklistService.blacklist(tokenId, Duration.ofMillis(ttlMs));
        assertTrue(tokenBlacklistService.isBlacklisted(tokenId));

        mockMvc.perform(get("/api/v1/evidence/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}

