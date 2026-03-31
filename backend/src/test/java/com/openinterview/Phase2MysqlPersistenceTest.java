package com.openinterview;

import com.openinterview.entity.AiResumeParseResultEntity;
import com.openinterview.entity.CandidateEntity;
import com.openinterview.entity.ExportTaskEntity;
import com.openinterview.entity.InterviewPlanEntity;
import com.openinterview.entity.OperationLogEntity;
import com.openinterview.mapper.CandidateMapper;
import com.openinterview.service.db.AiTaskDbService;
import com.openinterview.service.db.AuditLogDbService;
import com.openinterview.service.db.CandidateDbService;
import com.openinterview.service.db.ExportTaskDbService;
import com.openinterview.service.db.InterviewPlanDbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase2MysqlPersistenceTest {

    @Autowired
    private CandidateDbService candidateDbService;
    @Autowired
    private CandidateMapper candidateMapper;
    @Autowired
    private InterviewPlanDbService interviewPlanDbService;
    @Autowired
    private ExportTaskDbService exportTaskDbService;
    @Autowired
    private AiTaskDbService aiTaskDbService;
    @Autowired
    private AuditLogDbService auditLogDbService;

    @Test
    void candidateCrudAndLogicDelete() {
        CandidateEntity c = new CandidateEntity();
        c.candidateCode = "CAND-PH2-001";
        c.name = "张三";
        c.phone = "13800000001";
        c.applyPosition = "JAVA_ADV";
        c.status = 1;
        c.createUser = 1L;
        c.isDeleted = 0;

        CandidateEntity saved = candidateDbService.create(c);
        assertNotNull(saved.id);

        CandidateEntity loaded = candidateDbService.getById(saved.id);
        assertEquals("张三", loaded.name);

        loaded.name = "李四";
        assertTrue(candidateDbService.updateById(loaded));

        candidateMapper.deleteById(saved.id);
        assertNull(candidateDbService.getById(saved.id));
    }

    @Test
    void interviewPlanCrud() {
        InterviewPlanEntity p = new InterviewPlanEntity();
        p.interviewCode = "INT-PH2-001";
        p.candidateId = 100L;
        p.applyPosition = "JAVA_ADV";
        p.interviewRound = "R1";
        p.interviewType = 1;
        p.templateId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 1, 11, 0);
        p.interviewStartTime = start;
        p.interviewEndTime = end;
        p.interviewRoomId = "room-1";
        p.interviewRoomLink = "https://meet.example/1";
        p.hrUserId = 1L;
        p.interviewerIds = "2,3";
        p.interviewStatus = 1;
        p.isSigned = 0;
        p.isDeleted = 0;

        InterviewPlanEntity saved = interviewPlanDbService.create(p);
        assertNotNull(saved.id);

        InterviewPlanEntity byCode = interviewPlanDbService.getByInterviewCode("INT-PH2-001");
        assertNotNull(byCode);
        assertEquals(100L, byCode.candidateId);
    }

    @Test
    void exportTaskCrud() {
        ExportTaskEntity t = new ExportTaskEntity();
        t.taskCode = "EXP-PH2-001";
        t.exportType = 0;
        t.exportContent = "1,2,3";
        t.jobCode = "JOB01";
        t.exportUserId = 1L;
        t.exportUserName = "admin";
        t.taskStatus = 1;
        t.isDeleted = 0;

        ExportTaskEntity saved = exportTaskDbService.create(t);
        assertNotNull(saved.id);

        ExportTaskEntity byCode = exportTaskDbService.getByTaskCode("EXP-PH2-001");
        assertNotNull(byCode);
        assertEquals(0, byCode.exportType);
    }

    @Test
    void aiParseResultCrud() {
        AiResumeParseResultEntity e = new AiResumeParseResultEntity();
        e.candidateId = 5001L;
        e.resumeFileUrl = "mock://resume/5001.pdf";
        e.parseStatus = 2;
        e.basicInfoJson = "{\"name\":\"x\"}";
        e.isDeleted = 0;

        AiResumeParseResultEntity saved = aiTaskDbService.createParseResult(e);
        assertNotNull(saved.id);

        AiResumeParseResultEntity loaded = aiTaskDbService.getParseResultById(saved.id);
        assertNotNull(loaded);
        assertEquals(2, loaded.parseStatus);
    }

    @Test
    void auditLogWriteAndQuery() {
        OperationLogEntity log = new OperationLogEntity();
        log.logCode = "LOG-PH2-001";
        log.traceId = "trace-ph2-001";
        log.operationModule = "resume";
        log.operationType = "parse";
        log.operationDesc = "单元测试写入";
        log.operationTime = LocalDateTime.now();
        log.costTime = 12;
        log.operationStatus = 1;

        OperationLogEntity saved = auditLogDbService.create(log);
        assertNotNull(saved.id);

        assertEquals(1, auditLogDbService.listByTraceId("trace-ph2-001").size());
        assertEquals(1, auditLogDbService.listByModule("resume").size());
    }

    @Test
    void answerAssessCrud() {
        var e = new com.openinterview.entity.AnswerAssessRecordEntity();
        e.recordCode = "ANS-PH2-001";
        e.interviewId = 7001L;
        e.questionId = 8001L;
        e.candidateId = 9001L;
        e.answerText = "回答内容";
        e.accuracyScore = new BigDecimal("80.00");
        e.createUser = 1L;
        e.isDeleted = 0;

        var saved = aiTaskDbService.createAnswerAssess(e);
        assertNotNull(saved.id);
        assertEquals(1, aiTaskDbService.listAnswerAssessByInterviewId(7001L).size());
    }
}
