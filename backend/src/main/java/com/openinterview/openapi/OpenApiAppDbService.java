package com.openinterview.openapi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.OpenApiAppEntity;
import com.openinterview.mapper.OpenApiAppMapper;
import org.springframework.stereotype.Service;

@Service
public class OpenApiAppDbService {
    private final OpenApiAppMapper mapper;

    public OpenApiAppDbService(OpenApiAppMapper mapper) {
        this.mapper = mapper;
    }

    public OpenApiAppEntity getByAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<OpenApiAppEntity>()
                .eq("app_id", appId)
                .last("LIMIT 1"));
    }

    public boolean updateById(OpenApiAppEntity entity) {
        return mapper.updateById(entity) > 0;
    }
}

