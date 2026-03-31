package com.openinterview.export;

public interface ExportStrategy {
    /** 导出并返回生成的文件字节 */
    ExportResult export(ExportContext context);
}
