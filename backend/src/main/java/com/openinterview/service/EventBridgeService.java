package com.openinterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinterview.config.EventBridgeProperties;
import com.openinterview.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class EventBridgeService {
    private static final Logger log = LoggerFactory.getLogger(EventBridgeService.class);

    private final EventMappingService eventMappingService;
    private final EventBridgeProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final EvidenceStore evidenceStore;
    private final RestClient restClient;

    public EventBridgeService(EventMappingService eventMappingService,
                              EventBridgeProperties properties,
                              RabbitTemplate rabbitTemplate,
                              ObjectMapper objectMapper,
                              EvidenceStore evidenceStore) {
        this.eventMappingService = eventMappingService;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.evidenceStore = evidenceStore;
        this.restClient = RestClient.builder().build();
    }

    public EventMessage publish(String mqEventCode, String bizCode, Map<String, Object> payload) {
        return publish(mqEventCode, bizCode, payload, TraceContext.getTraceId());
    }

    public EventMessage publish(String mqEventCode, String bizCode, Map<String, Object> payload, String traceId) {
        EventMessage mqMsg = EventMessage.of(mqEventCode, traceId, bizCode, payload);
        evidenceStore.addMq(mqMsg);

        if (properties.isMqEnabled()) {
            rabbitTemplate.convertAndSend(properties.getExchange(), mqEventCode, toJson(mqMsg));
        }

        String webhookEventCode = eventMappingService.toWebhookEvent(mqEventCode);
        EventMessage webhookMsg = EventMessage.of(webhookEventCode, traceId, bizCode, payload);
        evidenceStore.addWebhook(webhookMsg);

        if (properties.isWebhookEnabled()) {
            boolean ok = postWebhook(webhookMsg);
            if (!ok) {
                webhookMsg.retryCount = 1;
                log.warn("webhook first attempt failed, retrying once traceId={} bizCode={} mq={} webhook={}",
                        traceId, bizCode, mqEventCode, webhookEventCode);
                ok = postWebhook(webhookMsg);
                if (!ok) {
                    String err = "WEBHOOK_DELIVERY_FAILED";
                    String reason = "HTTP callback failed after retries";
                    log.error("webhook delivery failed after retry traceId={} bizCode={} mq={} webhook={}",
                            traceId, bizCode, mqEventCode, webhookEventCode);
                    evidenceStore.addWebhookDeliveryFailure(traceId, bizCode, err, reason, mqEventCode, webhookEventCode);
                }
            }
        }
        return webhookMsg;
    }

    private boolean postWebhook(EventMessage webhookMsg) {
        try {
            restClient.post()
                    .uri(properties.getWebhookUrl())
                    .body(webhookMsg)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("webhook post failed traceId={} bizCode={} err={}",
                    webhookMsg.traceId, webhookMsg.bizCode, ex.getMessage());
            return false;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new RuntimeException("serialize event failed", ex);
        }
    }
}
