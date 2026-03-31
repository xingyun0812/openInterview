package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public IPage<InterviewPlanEntity> page(long current, long size, Integer interviewStatus) {
        Page<InterviewPlanEntity> p = new Page<>(current, size);
        QueryWrapper<InterviewPlanEntity> w = new QueryWrapper<>();
        if (interviewStatus != null) {
            w.eq("interview_status", interviewStatus);
        }
        w.orderByDesc("id");
        return interviewPlanMapper.selectPage(p, w);
    }
}
