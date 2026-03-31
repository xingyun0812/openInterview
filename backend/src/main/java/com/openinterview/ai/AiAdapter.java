package com.openinterview.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 适配层：对上提供稳定接口，对下可插拔不同 Provider。
 * <p>
 * 约束：默认必须可在无网络/无 API Key 的环境中工作（走 Mock）。
 */
public interface AiAdapter {

    ParseResumeOutput parseResume(ParseResumeInput input);

    List<Map<String, Object>> generateQuestions(GenerateQuestionsInput input);

    EvaluateAnswerOutput evaluateAnswer(EvaluateAnswerInput input);

    record ParseResumeInput(Long candidateId, String resumeUrl, byte[] content) {
    }

    record ParseResumeOutput(
            Map<String, Object> basicInfo,
            List<Map<String, Object>> education,
            List<Map<String, Object>> workExperience,
            List<String> skillTags
    ) {
    }

    record GenerateQuestionsInput(Long interviewId, String resumeSectionId, Integer difficulty, Integer questionCount) {
    }

    record EvaluateAnswerInput(Long interviewId, Long questionId, String answerText) {
    }

    record EvaluateAnswerOutput(
            String accuracyScore,
            String coverageScore,
            String clarityScore,
            String followUpSuggest
    ) {
    }
}

