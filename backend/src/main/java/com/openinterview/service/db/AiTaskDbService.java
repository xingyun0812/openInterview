package com.openinterview.service.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.openinterview.entity.AiQuestionGenerateRecordEntity;
import com.openinterview.entity.AiResumeParseResultEntity;
import com.openinterview.entity.AiResumeScreenResultEntity;
import com.openinterview.entity.AnswerAssessRecordEntity;
import com.openinterview.mapper.AiQuestionGenerateRecordMapper;
import com.openinterview.mapper.AiResumeParseResultMapper;
import com.openinterview.mapper.AiResumeScreenResultMapper;
import com.openinterview.mapper.AnswerAssessRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiTaskDbService {
    private final AiResumeParseResultMapper parseResultMapper;
    private final AiResumeScreenResultMapper screenResultMapper;
    private final AiQuestionGenerateRecordMapper questionRecordMapper;
    private final AnswerAssessRecordMapper answerAssessRecordMapper;

    public AiTaskDbService(
            AiResumeParseResultMapper parseResultMapper,
            AiResumeScreenResultMapper screenResultMapper,
            AiQuestionGenerateRecordMapper questionRecordMapper,
            AnswerAssessRecordMapper answerAssessRecordMapper) {
        this.parseResultMapper = parseResultMapper;
        this.screenResultMapper = screenResultMapper;
        this.questionRecordMapper = questionRecordMapper;
        this.answerAssessRecordMapper = answerAssessRecordMapper;
    }

    public AiResumeParseResultEntity createParseResult(AiResumeParseResultEntity entity) {
        parseResultMapper.insert(entity);
        return entity;
    }

    public AiResumeParseResultEntity getParseResultById(Long id) {
        return parseResultMapper.selectById(id);
    }

    public boolean updateParseResultById(AiResumeParseResultEntity entity) {
        return parseResultMapper.updateById(entity) > 0;
    }

    public List<AiResumeParseResultEntity> listParseByCandidateId(Long candidateId) {
        return parseResultMapper.selectList(
                new QueryWrapper<AiResumeParseResultEntity>().eq("candidate_id", candidateId));
    }

    public AiResumeScreenResultEntity createScreenResult(AiResumeScreenResultEntity entity) {
        screenResultMapper.insert(entity);
        return entity;
    }

    public AiResumeScreenResultEntity getScreenResultById(Long id) {
        return screenResultMapper.selectById(id);
    }

    public boolean updateScreenResultById(AiResumeScreenResultEntity entity) {
        return screenResultMapper.updateById(entity) > 0;
    }

    public List<AiResumeScreenResultEntity> listScreenByCandidateAndJob(Long candidateId, String jobCode) {
        return screenResultMapper.selectList(
                new QueryWrapper<AiResumeScreenResultEntity>()
                        .eq("candidate_id", candidateId)
                        .eq("job_code", jobCode));
    }

    public AiQuestionGenerateRecordEntity createQuestionRecord(AiQuestionGenerateRecordEntity entity) {
        questionRecordMapper.insert(entity);
        return entity;
    }

    public AiQuestionGenerateRecordEntity getQuestionRecordById(Long id) {
        return questionRecordMapper.selectById(id);
    }

    public boolean updateQuestionRecordById(AiQuestionGenerateRecordEntity entity) {
        return questionRecordMapper.updateById(entity) > 0;
    }

    public AiQuestionGenerateRecordEntity getQuestionByRequestCode(String requestCode) {
        return questionRecordMapper.selectOne(
                new QueryWrapper<AiQuestionGenerateRecordEntity>()
                        .eq("request_code", requestCode)
                        .last("LIMIT 1"));
    }

    public AnswerAssessRecordEntity createAnswerAssess(AnswerAssessRecordEntity entity) {
        answerAssessRecordMapper.insert(entity);
        return entity;
    }

    public AnswerAssessRecordEntity getAnswerAssessById(Long id) {
        return answerAssessRecordMapper.selectById(id);
    }

    public boolean updateAnswerAssessById(AnswerAssessRecordEntity entity) {
        return answerAssessRecordMapper.updateById(entity) > 0;
    }

    public List<AnswerAssessRecordEntity> listAnswerAssessByInterviewId(Long interviewId) {
        return answerAssessRecordMapper.selectList(
                new QueryWrapper<AnswerAssessRecordEntity>().eq("interview_id", interviewId));
    }
}
