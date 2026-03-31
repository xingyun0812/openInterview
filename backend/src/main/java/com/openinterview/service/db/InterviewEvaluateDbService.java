package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.InterviewEvaluateEntity;
import com.openinterview.mapper.InterviewEvaluateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewEvaluateDbService {
    private final InterviewEvaluateMapper mapper;

    public InterviewEvaluateDbService(InterviewEvaluateMapper mapper) {
        this.mapper = mapper;
    }

    public InterviewEvaluateEntity getById(Long id) {
        return mapper.selectById(id);
    }

    public InterviewEvaluateEntity getByInterviewAndInterviewer(Long interviewId, Long interviewerId) {
        return mapper.selectOne(new QueryWrapper<InterviewEvaluateEntity>()
                .eq("interview_id", interviewId)
                .eq("interviewer_id", interviewerId)
                .last("LIMIT 1"));
    }

    public List<InterviewEvaluateEntity> listByInterviewId(Long interviewId) {
        return mapper.selectList(new QueryWrapper<InterviewEvaluateEntity>().eq("interview_id", interviewId));
    }

    public InterviewEvaluateEntity create(InterviewEvaluateEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public boolean updateById(InterviewEvaluateEntity entity) {
        return mapper.updateById(entity) > 0;
    }
}

