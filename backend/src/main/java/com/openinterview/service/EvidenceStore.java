package com.openinterview.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class EvidenceStore {
    private final List<EventMessage> mqEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<EventMessage> webhookEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> exportAudits = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> webhookDeliveryFailures = Collections.synchronizedList(new ArrayList<>());

    public void addMq(EventMessage event) {
        mqEvents.add(event);
    }

    public void addWebhook(EventMessage event) {
        webhookEvents.add(event);
    }

    public List<EventMessage> getMqEvents() {
        return new ArrayList<>(mqEvents);
    }

    public List<EventMessage> getWebhookEvents() {
        return new ArrayList<>(webhookEvents);
    }

    public void addExportAudit(Map<String, Object> audit) {
        exportAudits.add(audit);
    }

    public List<Map<String, Object>> getExportAudits() {
        return new ArrayList<>(exportAudits);
    }

    /**
     * Webhook 回调在重试后仍失败时记录，便于按 traceId / bizCode 追溯。
     */
    public void addWebhookDeliveryFailure(String traceId,
                                          String bizCode,
                                          String errorCode,
                                          String failReason,
                                          String mqEventCode,
                                          String webhookEventCode) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("traceId", traceId);
        row.put("bizCode", bizCode);
        row.put("errorCode", errorCode);
        row.put("failReason", failReason);
        row.put("mqEventCode", mqEventCode);
        row.put("webhookEventCode", webhookEventCode);
        row.put("occurTime", java.time.LocalDateTime.now().toString());
        webhookDeliveryFailures.add(row);
    }

    public List<Map<String, Object>> getWebhookDeliveryFailures() {
        return new ArrayList<>(webhookDeliveryFailures);
    }
}
