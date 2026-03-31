package com.openinterview.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class Phase2AiAdapterTest {

    @SpringBootTest
    @TestPropertySource(properties = {
            "ai.enabled=true",
            "ai.provider=mock",
            "ai.openai.api-key="
    })
    static class MockDefaultTest {
        @Autowired
        private AiAdapterFactory factory;

        @Autowired
        private AiAdapter aiAdapter;

        @Test
        void factory_shouldReturnMock_byDefault() {
            assertThat(factory.getAdapter()).isInstanceOf(MockAiAdapter.class);
            assertThat(aiAdapter).isInstanceOf(MockAiAdapter.class);
        }

        @Test
        void mockOutput_shouldBeStable() {
            AiAdapter.ParseResumeOutput parse = aiAdapter.parseResume(new AiAdapter.ParseResumeInput(1L, "mock://resume/1/a.pdf", new byte[]{1, 2, 3}));
            assertThat(parse.basicInfo()).containsEntry("candidateId", 1L).containsEntry("name", "候选人1");
            assertThat(parse.skillTags()).containsExactly("Java", "Spring Boot", "MySQL");

            var qs = aiAdapter.generateQuestions(new AiAdapter.GenerateQuestionsInput(10L, "secA", 2, 2));
            assertThat(qs).hasSize(2);
            assertThat(qs.get(0)).containsKey("stem");
            assertThat(qs.get(0)).containsKey("referenceAnswer");
            assertThat(qs.get(0)).containsKey("rubricPoints");
            assertThat(qs.get(0)).containsKey("followUps");

            AiAdapter.EvaluateAnswerOutput eval = aiAdapter.evaluateAnswer(new AiAdapter.EvaluateAnswerInput(10L, 20L, "answer"));
            assertThat(eval.accuracyScore()).isEqualTo("82.50");
            assertThat(eval.followUpSuggest()).contains("容量评估");
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {
            "ai.enabled=true",
            "ai.provider=openai",
            "ai.openai.api-key="
    })
    static class OpenAiNoKeyFallbackTest {
        @Autowired
        private AiAdapterFactory factory;

        @Autowired
        private AiAdapter aiAdapter;

        @Test
        void providerOpenAi_withoutKey_shouldFallbackToMock() {
            assertThat(factory.getAdapter()).isInstanceOf(MockAiAdapter.class);
            assertThat(aiAdapter).isInstanceOf(MockAiAdapter.class);
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = {
            "ai.enabled=true",
            "ai.provider=openai",
            "ai.openai.api-key=dummy-key"
    })
    static class OpenAiWithKeySelectionTest {
        @Autowired
        private AiAdapterFactory factory;

        @Autowired
        private AiAdapter aiAdapter;

        @Test
        void providerOpenAi_withKey_shouldSelectOpenAiAdapter() {
            assertThat(factory.getAdapter()).isInstanceOf(OpenAiAdapter.class);
            assertThat(aiAdapter).isInstanceOf(OpenAiAdapter.class);
        }
    }
}

