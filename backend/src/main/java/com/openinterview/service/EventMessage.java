package com.openinterview.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class EventMessage {
    public String eventCode;
    public String traceId;
    public String bizCode;
    public String occurTime;
    public int retryCount;
    public Map<String, Object> payload;

    public static EventMessage of(String eventCode, String traceId, String bizCode, Map<String, Object> payload) {
        EventMessage msg = new EventMessage();
        msg.eventCode = eventCode;
        msg.traceId = traceId;
        msg.bizCode = bizCode;
        msg.occurTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        msg.retryCount = 0;
        msg.payload = payload;
        return msg;
    }
}
