package com.openinterview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "event.bridge")
public class EventBridgeProperties {
    private boolean mqEnabled = false;
    private boolean webhookEnabled = false;
    private String exchange = "interview.direct";
    private String webhookUrl = "http://localhost:8080/webhook/mock/receive";

    public boolean isMqEnabled() {
        return mqEnabled;
    }

    public void setMqEnabled(boolean mqEnabled) {
        this.mqEnabled = mqEnabled;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
