package com.openinterview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.dto.auth.AuthResponse;
import com.openinterview.dto.auth.LoginRequest;
import com.openinterview.dto.auth.RefreshTokenRequest;
import com.openinterview.dto.auth.RegisterRequest;
import com.openinterview.entity.SysRoleEntity;
import com.openinterview.entity.SysUserEntity;
import com.openinterview.mapper.SysRoleMapper;
import com.openinterview.mapper.SysUserMapper;
import com.openinterview.security.JwtProperties;
import com.openinterview.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_REGISTER_ROLE = "INTERVIEWER";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       PasswordEncoder passwordEncoder,
                       UserDetailsService userDetailsService,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse login(LoginRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "AUTH", "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "AUTH", "用户名或密码错误");
        }
        return buildTokens(userDetails);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Long n = sysUserMapper.selectCount(
                new QueryWrapper<SysUserEntity>().eq("username", request.getUsername()));
        if (n != null && n > 0) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "AUTH", "用户名已存在");
        }
        SysRoleEntity role = sysRoleMapper.selectOne(
                new QueryWrapper<SysRoleEntity>().eq("role_code", DEFAULT_REGISTER_ROLE));
        if (role == null) {
            throw new ApiException(ErrorCode.SYSTEM_ERROR, "AUTH", "系统未配置默认角色 " + DEFAULT_REGISTER_ROLE);
        }
        SysUserEntity user = new SysUserEntity();
        user.username = request.getUsername();
        user.password = passwordEncoder.encode(request.getPassword());
        user.realName = request.getRealName() != null && !request.getRealName().isBlank()
                ? request.getRealName()
                : request.getUsername();
        user.roleId = role.id;
        user.status = 1;
        sysUserMapper.insert(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.username);
        return buildTokens(userDetails);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refresh = request.getRefreshToken();
        if (!jwtTokenProvider.validateRefreshToken(refresh)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "AUTH", "刷新令牌无效或已过期");
        }
        String username = jwtTokenProvider.getUsernameFromToken(refresh);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return buildTokens(userDetails);
    }

    private AuthResponse buildTokens(UserDetails userDetails) {
        String roleCode = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        String access = jwtTokenProvider.generateToken(userDetails.getUsername(), roleCode);
        String refresh = jwtTokenProvider.generateRefreshToken(userDetails.getUsername());
        return new AuthResponse(access, refresh, "Bearer", jwtProperties.getExpirationMs());
    }
}
