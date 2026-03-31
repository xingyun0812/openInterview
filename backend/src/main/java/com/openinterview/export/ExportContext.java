package com.openinterview.export;

/**
 * 真实导出上下文（与 {@link com.openinterview.service.InMemoryWorkflowService} 的 exportType 约定一致：0/1/2）。
 */
public class ExportContext {
    public int exportType;
    /** 候选人 ID 或面试 ID 列表，逗号分隔 */
    public String exportContent;
    public String jobCode;
    public String taskCode;
}
