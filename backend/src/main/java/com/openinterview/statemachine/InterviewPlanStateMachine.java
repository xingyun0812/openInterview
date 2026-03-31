package com.openinterview.statemachine;

/**
 * 面试计划状态码（与 interview_plan.interview_status 一致）。
 * 说明：若业务上存在「已发布」概念，当前模型未单独建状态，与「待面试」复用同一码值 {@link #PENDING}。
 */
public enum InterviewPlanStateMachine {
    PENDING(1),
    IN_PROGRESS(2),
    COMPLETED(3),
    CANCELLED(4),
    OVERDUE(5);

    private final int code;

    InterviewPlanStateMachine(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static InterviewPlanStateMachine fromCode(int code) {
        for (InterviewPlanStateMachine s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown interview status code: " + code);
    }
}
