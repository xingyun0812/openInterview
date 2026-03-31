package com.openinterview.export;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ExportStrategyFactory {

    private final ScreeningExcelStrategy screeningExcelStrategy;
    private final ScoreExcelStrategy scoreExcelStrategy;
    private final InterviewWordStrategy interviewWordStrategy;

    public ExportStrategyFactory(ScreeningExcelStrategy screeningExcelStrategy,
                                 ScoreExcelStrategy scoreExcelStrategy,
                                 InterviewWordStrategy interviewWordStrategy) {
        this.screeningExcelStrategy = screeningExcelStrategy;
        this.scoreExcelStrategy = scoreExcelStrategy;
        this.interviewWordStrategy = interviewWordStrategy;
    }

    public ExportStrategy getStrategy(int exportType) {
        return switch (exportType) {
            case 0 -> screeningExcelStrategy;
            case 1 -> scoreExcelStrategy;
            case 2 -> interviewWordStrategy;
            default -> throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_TYPE", "exportType 仅支持 0/1/2");
        };
    }
}
