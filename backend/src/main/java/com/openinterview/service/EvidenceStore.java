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
    private volatile RegressionSnapshot regressionSnapshot;

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
     * 覆盖式写入回归测试快照（用于门禁 gate4 或单测注入；优先级高于 surefire 解析）。
     */
    public void setRegressionSnapshot(int totalTests, int failureCount, List<String> failureNames) {
        RegressionSnapshot snap = new RegressionSnapshot();
        snap.totalTests = totalTests;
        snap.failureCount = failureCount;
        snap.failureNames = failureNames == null ? List.of() : List.copyOf(failureNames);
        this.regressionSnapshot = snap;
    }

    public RegressionSnapshot getRegressionSnapshot() {
        return regressionSnapshot;
    }

    public void clearRegressionSnapshot() {
        this.regressionSnapshot = null;
    }

    /**
     * 清空 MQ/Webhook/导出审计与回归快照（供门禁单测隔离使用）。
     */
    public void clearAll() {
        synchronized (mqEvents) {
            mqEvents.clear();
            webhookEvents.clear();
            exportAudits.clear();
        }
        regressionSnapshot = null;
    }

    public static class RegressionSnapshot {
        public int totalTests;
        public int failureCount;
        public List<String> failureNames = List.of();
    }
}
