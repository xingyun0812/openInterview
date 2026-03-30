package com.openinterview.service;

import com.openinterview.trace.TraceContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AuditTrailService {
    private final List<AuditRecord> records = Collections.synchronizedList(new ArrayList<>());

    public void record(String module, String action, String bizCode, String errorCode, String detail) {
        AuditRecord record = new AuditRecord();
        record.module = module;
        record.action = action;
        record.traceId = TraceContext.getTraceId();
        record.bizCode = bizCode;
        record.errorCode = errorCode;
        record.detail = detail;
        record.occurTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        records.add(record);
    }

    public List<AuditRecord> list() {
        return new ArrayList<>(records);
    }

    public static class AuditRecord {
        public String module;
        public String action;
        public String traceId;
        public String bizCode;
        public String errorCode;
        public String detail;
        public String occurTime;
    }
}
