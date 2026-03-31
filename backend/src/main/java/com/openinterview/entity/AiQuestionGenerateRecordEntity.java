package com.openinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_question_generate_record")
public class AiQuestionGenerateRecordEntity {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String requestCode;
    public Long interviewId;
    public String resumeSectionId;
    public String inputSnapshotHash;
    public String applyPosition;
    public Integer difficultyLevel;
    public String techStack;
    public Integer questionCount;
    public String questionPayloadJson;
    public Integer reviewStatus;
    public Long reviewUserId;
    public String reviewComment;
    public String modelName;
    public Long createUser;
    public LocalDateTime createTime;
    public LocalDateTime updateTime;
    @TableLogic
    public Integer isDeleted;
}
