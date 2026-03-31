package com.openinterview.export;

import com.alibaba.excel.annotation.ExcelProperty;

import java.math.BigDecimal;

public class ScreeningExcelRow {
    @ExcelProperty("候选人姓名")
    private String candidateName;
    @ExcelProperty("应聘岗位")
    private String applyPosition;
    @ExcelProperty("匹配分")
    private BigDecimal matchScore;
    @ExcelProperty("推荐等级")
    private String recommendLevel;
    @ExcelProperty("复核结论")
    private String reviewResult;
    @ExcelProperty("复核人")
    private String reviewUser;
    @ExcelProperty("复核时间")
    private String reviewTime;

    public ScreeningExcelRow() {
    }

    public ScreeningExcelRow(String candidateName, String applyPosition, BigDecimal matchScore,
                             String recommendLevel, String reviewResult, String reviewUser, String reviewTime) {
        this.candidateName = candidateName;
        this.applyPosition = applyPosition;
        this.matchScore = matchScore;
        this.recommendLevel = recommendLevel;
        this.reviewResult = reviewResult;
        this.reviewUser = reviewUser;
        this.reviewTime = reviewTime;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getApplyPosition() {
        return applyPosition;
    }

    public void setApplyPosition(String applyPosition) {
        this.applyPosition = applyPosition;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public String getRecommendLevel() {
        return recommendLevel;
    }

    public void setRecommendLevel(String recommendLevel) {
        this.recommendLevel = recommendLevel;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getReviewUser() {
        return reviewUser;
    }

    public void setReviewUser(String reviewUser) {
        this.reviewUser = reviewUser;
    }

    public String getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(String reviewTime) {
        this.reviewTime = reviewTime;
    }
}
