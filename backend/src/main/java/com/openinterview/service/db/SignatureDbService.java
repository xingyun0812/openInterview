package com.openinterview.service.db;

import com.openinterview.entity.SignatureEntity;
import com.openinterview.mapper.SignatureMapper;
import org.springframework.stereotype.Service;

@Service
public class SignatureDbService {
    private final SignatureMapper signatureMapper;

    public SignatureDbService(SignatureMapper signatureMapper) {
        this.signatureMapper = signatureMapper;
    }

    public SignatureEntity create(SignatureEntity entity) {
        signatureMapper.insert(entity);
        return entity;
    }

    public SignatureEntity getById(Long id) {
        return signatureMapper.selectById(id);
    }

    public boolean updateById(SignatureEntity entity) {
        return signatureMapper.updateById(entity) > 0;
    }
}

