package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.ExportTaskEntity;
import com.openinterview.mapper.ExportTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExportTaskDbService {
    private final ExportTaskMapper exportTaskMapper;

    public ExportTaskDbService(ExportTaskMapper exportTaskMapper) {
        this.exportTaskMapper = exportTaskMapper;
    }

    public ExportTaskEntity create(ExportTaskEntity entity) {
        exportTaskMapper.insert(entity);
        return entity;
    }

    public ExportTaskEntity getById(Long id) {
        return exportTaskMapper.selectById(id);
    }

    public boolean updateById(ExportTaskEntity entity) {
        return exportTaskMapper.updateById(entity) > 0;
    }

    public ExportTaskEntity getByTaskCode(String taskCode) {
        return exportTaskMapper.selectOne(
                new QueryWrapper<ExportTaskEntity>().eq("task_code", taskCode).last("LIMIT 1"));
    }

    public List<ExportTaskEntity> listByTaskStatus(Integer taskStatus) {
        return exportTaskMapper.selectList(new QueryWrapper<ExportTaskEntity>().eq("task_status", taskStatus));
    }
}
