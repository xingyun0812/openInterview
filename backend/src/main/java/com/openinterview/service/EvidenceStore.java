package com.openinterview.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class EvidenceStore {
    private final List<EventMessage> mqEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<EventMessage> webhookEvents = Collections.synchronizedList(new ArrayList<>());

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
}
