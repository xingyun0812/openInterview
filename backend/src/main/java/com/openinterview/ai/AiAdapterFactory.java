package com.openinterview.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiAdapterFactory {
    private final AiProperties properties;
    private final MockAiAdapter mockAiAdapter;

    public AiAdapterFactory(AiProperties properties, MockAiAdapter mockAiAdapter) {
        this.properties = properties;
        this.mockAiAdapter = mockAiAdapter;
    }

    public AiAdapter getAdapter() {
        if (properties == null || !properties.isEnabled()) {
            return mockAiAdapter;
        }

        String provider = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if ("openai".equals(provider)) {
            String apiKey = properties.getOpenai() == null ? null : properties.getOpenai().getApiKey();
            if (StringUtils.hasText(apiKey)) {
                return new OpenAiAdapter(apiKey);
            }
            return mockAiAdapter;
        }

        return mockAiAdapter;
    }
}

