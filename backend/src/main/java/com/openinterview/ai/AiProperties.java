package com.openinterview.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /**
     * 总开关：即使开启，也必须在无法满足条件时回退到 mock。
     */
    private boolean enabled = true;

    /**
     * mock | openai
     */
    private String provider = "mock";

    private OpenAi openai = new OpenAi();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public static class OpenAi {
        /**
         * 从环境变量读取：${OPENAI_API_KEY:}
         * 为空时强制回退 mock，且不允许触发外部请求。
         */
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}

