package com.openinterview.common;

import com.openinterview.trace.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApiException(ApiException ex) {
        HttpStatus status;
        if (ex.getErrorCode() == ErrorCode.EXPORT_TASK_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getErrorCode() == ErrorCode.UNAUTHORIZED) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.getErrorCode() == ErrorCode.FORBIDDEN) {
            status = HttpStatus.FORBIDDEN;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(Result.fail(ex.getErrorCode(), TraceContext.getTraceId(), ex.getBizCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidateException(MethodArgumentNotValidException ex) {
        return Result.fail(ErrorCode.PARAM_INVALID, TraceContext.getTraceId(), "VALIDATION", ex.getMessage());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBadRequest(Exception ex) {
        return Result.fail(ErrorCode.PARAM_INVALID, TraceContext.getTraceId(), "VALIDATION", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(ErrorCode.SYSTEM_ERROR, TraceContext.getTraceId(), "UNEXPECTED", ex.getMessage());
    }
}
