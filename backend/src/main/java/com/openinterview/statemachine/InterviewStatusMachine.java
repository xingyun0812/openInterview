package com.openinterview.statemachine;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import com.openinterview.entity.InterviewPlanEntity;

/**
 * 校验面试计划状态迁移是否合法，非法则抛出 {@link ApiException}。
 */
public final class InterviewStatusMachine {

    private InterviewStatusMachine() {
    }

    /**
     * 允许的迁移：
     * <ul>
     *   <li>PENDING → IN_PROGRESS（开始面试）</li>
     *   <li>IN_PROGRESS → COMPLETED（结束面试）</li>
     *   <li>PENDING / IN_PROGRESS → CANCELLED（取消）</li>
     *   <li>PENDING → OVERDUE（逾期标记）</li>
     * </ul>
     */
    public static void transit(InterviewPlanEntity plan, int targetStatus) {
        if (plan == null || plan.interviewStatus == null) {
            throw new ApiException(ErrorCode.INTERVIEW_STATUS_ILLEGAL, "INT_STATUS", "面试状态非法");
        }
        int cur = plan.interviewStatus;
        boolean ok = switch (targetStatus) {
            case 2 -> cur == 1;
            case 3 -> cur == 2;
            case 4 -> cur == 1 || cur == 2;
            case 5 -> cur == 1;
            default -> false;
        };
        if (!ok) {
            String code = plan.interviewCode != null ? plan.interviewCode : String.valueOf(plan.id);
            throw new ApiException(
                    ErrorCode.INTERVIEW_STATUS_ILLEGAL,
                    code,
                    "非法状态转换: " + cur + " -> " + targetStatus);
        }
    }
}
