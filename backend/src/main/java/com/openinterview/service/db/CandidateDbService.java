package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.CandidateEntity;
import com.openinterview.mapper.CandidateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateDbService {
    private final CandidateMapper candidateMapper;

    public CandidateDbService(CandidateMapper candidateMapper) {
        this.candidateMapper = candidateMapper;
    }

    public CandidateEntity create(CandidateEntity entity) {
        candidateMapper.insert(entity);
        return entity;
    }

    public CandidateEntity getById(Long id) {
        return candidateMapper.selectById(id);
    }

    public boolean updateById(CandidateEntity entity) {
        return candidateMapper.updateById(entity) > 0;
    }

    public List<CandidateEntity> listByApplyPosition(String applyPosition) {
        return candidateMapper.selectList(new QueryWrapper<CandidateEntity>().eq("apply_position", applyPosition));
    }

    public List<CandidateEntity> listByPhone(String phone) {
        return candidateMapper.selectList(new QueryWrapper<CandidateEntity>().eq("phone", phone));
    }
}
