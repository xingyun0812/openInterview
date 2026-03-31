package com.openinterview.export;

public class ExportResult {
    public byte[] fileBytes;
    public String fileName;
    public String contentType;

    public ExportResult(byte[] fileBytes, String fileName, String contentType) {
        this.fileBytes = fileBytes;
        this.fileName = fileName;
        this.contentType = contentType;
    }
}
