package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.InterviewPlanEntity;
import com.openinterview.mapper.InterviewPlanMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewPlanDbService {
    private final InterviewPlanMapper interviewPlanMapper;

    public InterviewPlanDbService(InterviewPlanMapper interviewPlanMapper) {
        this.interviewPlanMapper = interviewPlanMapper;
    }

    public InterviewPlanEntity create(InterviewPlanEntity entity) {
        interviewPlanMapper.insert(entity);
        return entity;
    }

    public InterviewPlanEntity getById(Long id) {
        return interviewPlanMapper.selectById(id);
    }

    public boolean updateById(InterviewPlanEntity entity) {
        return interviewPlanMapper.updateById(entity) > 0;
    }

    public List<InterviewPlanEntity> listByCandidateId(Long candidateId) {
        return interviewPlanMapper.selectList(new QueryWrapper<InterviewPlanEntity>().eq("candidate_id", candidateId));
    }

    public InterviewPlanEntity getByInterviewCode(String interviewCode) {
        return interviewPlanMapper.selectOne(
                new QueryWrapper<InterviewPlanEntity>().eq("interview_code", interviewCode).last("LIMIT 1"));
    }
}
