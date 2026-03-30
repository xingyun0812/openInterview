package com.openinterview.common;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String bizCode;

    public ApiException(ErrorCode errorCode, String bizCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.bizCode = bizCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getBizCode() {
        return bizCode;
    }
}
