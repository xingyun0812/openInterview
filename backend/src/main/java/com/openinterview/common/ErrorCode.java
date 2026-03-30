package com.openinterview.common;

public enum ErrorCode {
    PARAM_INVALID(1001),
    INTERVIEW_NOT_FOUND(4007),
    UNAUTHORIZED(1002),
    FORBIDDEN(1003),
    EXPORT_TASK_NOT_FOUND(6001),
    EXPORT_FILE_FAILED(6003),
    RESUME_PARSE_FAILED(8001),
    RESUME_SCREEN_FAILED(8002),
    QUESTION_REVIEW_STATUS_ILLEGAL(4009),
    AI_REVIEW_REQUIRED(8004),
    ANSWER_EVALUATE_FAILED(8005),
    SCREEN_STATUS_ILLEGAL(8006),
    SYSTEM_ERROR(9001);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
