package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.OperationLogEntity;
import com.openinterview.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogDbService {
    private final OperationLogMapper operationLogMapper;

    public AuditLogDbService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public OperationLogEntity create(OperationLogEntity entity) {
        operationLogMapper.insert(entity);
        return entity;
    }

    public OperationLogEntity getById(Long id) {
        return operationLogMapper.selectById(id);
    }

    public boolean updateById(OperationLogEntity entity) {
        return operationLogMapper.updateById(entity) > 0;
    }

    public List<OperationLogEntity> listByTraceId(String traceId) {
        return operationLogMapper.selectList(new QueryWrapper<OperationLogEntity>().eq("trace_id", traceId));
    }

    public List<OperationLogEntity> listByModule(String operationModule) {
        return operationLogMapper.selectList(
                new QueryWrapper<OperationLogEntity>().eq("operation_module", operationModule));
    }
}
