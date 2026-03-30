package com.openinterview.common;

import com.openinterview.trace.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleApiException(ApiException ex) {
        return Result.fail(ex.getErrorCode(), TraceContext.getTraceId(), ex.getBizCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidateException(MethodArgumentNotValidException ex) {
        return Result.fail(ErrorCode.PARAM_INVALID, TraceContext.getTraceId(), "VALIDATION", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(ErrorCode.SYSTEM_ERROR, TraceContext.getTraceId(), "UNEXPECTED", ex.getMessage());
    }
}
