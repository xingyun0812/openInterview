package com.openinterview.common;

public class Result<T> {
    private int code;
    private String msg;
    private T data;
    private long timestamp;
    private String traceId;
    private String bizCode;
    private String errorCode;

    public static <T> Result<T> success(T data, String traceId, String bizCode) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "操作成功";
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        result.traceId = traceId;
        result.bizCode = bizCode;
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String traceId, String bizCode, String msg) {
        Result<T> result = new Result<>();
        result.code = errorCode.getCode();
        result.msg = msg;
        result.timestamp = System.currentTimeMillis();
        result.traceId = traceId;
        result.bizCode = bizCode;
        result.errorCode = String.valueOf(errorCode.getCode());
        return result;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getBizCode() {
        return bizCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
