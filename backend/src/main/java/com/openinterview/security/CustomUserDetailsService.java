package com.openinterview.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.SysRoleEntity;
import com.openinterview.entity.SysUserEntity;
import com.openinterview.mapper.SysRoleMapper;
import com.openinterview.mapper.SysUserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    public CustomUserDetailsService(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserEntity user = sysUserMapper.selectOne(
                new QueryWrapper<SysUserEntity>().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.status != null && user.status != 1) {
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }
        SysRoleEntity role = user.roleId != null ? sysRoleMapper.selectById(user.roleId) : null;
        String roleCode = role != null && role.roleCode != null ? role.roleCode : "USER";
        return User.builder()
                .username(user.username)
                .password(user.password)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .build();
    }
}
