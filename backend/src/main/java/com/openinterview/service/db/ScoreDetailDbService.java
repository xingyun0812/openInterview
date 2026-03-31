package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.ScoreDetailEntity;
import com.openinterview.mapper.ScoreDetailMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreDetailDbService {
    private final ScoreDetailMapper mapper;

    public ScoreDetailDbService(ScoreDetailMapper mapper) {
        this.mapper = mapper;
    }

    public List<ScoreDetailEntity> listByEvaluateId(Long evaluateId) {
        return mapper.selectList(new QueryWrapper<ScoreDetailEntity>().eq("evaluate_id", evaluateId));
    }

    public List<ScoreDetailEntity> listByInterviewId(Long interviewId) {
        return mapper.selectList(new QueryWrapper<ScoreDetailEntity>().eq("interview_id", interviewId));
    }

    public int deleteByEvaluateId(Long evaluateId) {
        return mapper.delete(new QueryWrapper<ScoreDetailEntity>().eq("evaluate_id", evaluateId));
    }

    public ScoreDetailEntity create(ScoreDetailEntity entity) {
        mapper.insert(entity);
        return entity;
    }
}

