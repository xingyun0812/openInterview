package com.openinterview.ai;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Provider（可插拔实现）。
 * <p>
 * 约束：默认不允许对外发请求；没有 key 时 factory 必须回退到 mock。
 * <p>
 * 当前实现以“可插拔骨架”为主：在满足 key 与开关时才允许被选中；
 * 具体的 Prompt/结构化解析会在后续 Phase 中补齐。
 */
public class OpenAiAdapter implements AiAdapter {
    private final String apiKey;

    public OpenAiAdapter(String apiKey) {
        this.apiKey = apiKey;
    }

    private void assertReady() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY 为空，禁止调用外部 Provider");
        }
    }

    @Override
    public ParseResumeOutput parseResume(ParseResumeInput input) {
        assertReady();
        throw new UnsupportedOperationException("OpenAiAdapter.parseResume 尚未实现（占位实现，避免默认触发外部请求）");
    }

    @Override
    public List<Map<String, Object>> generateQuestions(GenerateQuestionsInput input) {
        assertReady();
        throw new UnsupportedOperationException("OpenAiAdapter.generateQuestions 尚未实现（占位实现，避免默认触发外部请求）");
    }

    @Override
    public EvaluateAnswerOutput evaluateAnswer(EvaluateAnswerInput input) {
        assertReady();
        throw new UnsupportedOperationException("OpenAiAdapter.evaluateAnswer 尚未实现（占位实现，避免默认触发外部请求）");
    }
}

