package com.openinterview.export;

import com.alibaba.excel.annotation.ExcelProperty;

import java.math.BigDecimal;

public class ScoreExcelRow {
    @ExcelProperty("面试ID")
    private Long interviewId;
    @ExcelProperty("候选人姓名")
    private String candidateName;
    @ExcelProperty("最终得分")
    private BigDecimal finalScore;
    @ExcelProperty("面试结果")
    private String interviewResult;
    @ExcelProperty("评价意见")
    private String evaluatorComment;

    public ScoreExcelRow() {
    }

    public ScoreExcelRow(Long interviewId, String candidateName, BigDecimal finalScore,
                         String interviewResult, String evaluatorComment) {
        this.interviewId = interviewId;
        this.candidateName = candidateName;
        this.finalScore = finalScore;
        this.interviewResult = interviewResult;
        this.evaluatorComment = evaluatorComment;
    }

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getInterviewResult() {
        return interviewResult;
    }

    public void setInterviewResult(String interviewResult) {
        this.interviewResult = interviewResult;
    }

    public String getEvaluatorComment() {
        return evaluatorComment;
    }

    public void setEvaluatorComment(String evaluatorComment) {
        this.evaluatorComment = evaluatorComment;
    }
}
