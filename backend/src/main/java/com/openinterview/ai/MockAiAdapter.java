package com.openinterview.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MockAiAdapter implements AiAdapter {

    @Override
    public ParseResumeOutput parseResume(ParseResumeInput input) {
        Long candidateId = input == null ? null : input.candidateId();
        Map<String, Object> basicInfo = Map.of(
                "candidateId", candidateId,
                "name", "候选人" + candidateId
        );
        List<Map<String, Object>> education = List.of(Map.of("school", "示例大学", "degree", "本科"));
        List<Map<String, Object>> workExperience = List.of(Map.of("company", "示例科技", "title", "后端工程师"));
        List<String> skillTags = List.of("Java", "Spring Boot", "MySQL");
        return new ParseResumeOutput(basicInfo, education, workExperience, skillTags);
    }

    @Override
    public List<Map<String, Object>> generateQuestions(GenerateQuestionsInput input) {
        Long interviewId = input == null ? null : input.interviewId();
        String resumeSectionId = input == null ? null : input.resumeSectionId();
        int difficulty = input != null && input.difficulty() != null ? input.difficulty() : 1;
        int questionCount = input != null && input.questionCount() != null ? input.questionCount() : 1;

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= questionCount; i++) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("stem", "（AI建议）请结合片段 " + resumeSectionId + " 回答第 " + i + " 题（难度 " + difficulty + "，面试 " + interviewId + "）");
            q.put("referenceAnswer", "（AI建议，非业务终态）示例要点：分层、一致性、可观测性。");
            q.put("rubricPoints", List.of("架构清晰", "边界与异常", "可运维性"));
            q.put("followUps", List.of("若流量翻倍如何扩容？"));
            list.add(q);
        }
        return list;
    }

    @Override
    public EvaluateAnswerOutput evaluateAnswer(EvaluateAnswerInput input) {
        return new EvaluateAnswerOutput(
                "82.50",
                "78.00",
                "80.00",
                "请补充系统设计中的容量评估与降级方案。"
        );
    }
}

