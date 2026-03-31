package com.openinterview.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScreeningExcelStrategy implements ExportStrategy {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public ExportResult export(ExportContext context) {
        if (context.jobCode == null || context.jobCode.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "岗位编码不能为空");
        }
        List<Long> candidateIds = ExportContentParser.parseIds(context.exportContent);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<ScreeningExcelRow> rows = new ArrayList<>();
        for (Long candidateId : candidateIds) {
            if (candidateId <= 0) {
                throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "候选人ID非法");
            }
            rows.add(new ScreeningExcelRow(
                    "候选人" + candidateId,
                    context.jobCode,
                    BigDecimal.valueOf(80 + (candidateId % 20)).setScale(2, java.math.RoundingMode.HALF_UP),
                    candidateId % 3 == 0 ? "待定" : "推荐",
                    candidateId % 2 == 0 ? "通过筛选" : "待定",
                    "HR-" + (candidateId % 10),
                    now
            ));
        }

        WriteCellStyle head = new WriteCellStyle();
        head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        head.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        head.setWriteFont(headFont);
        WriteCellStyle content = new WriteCellStyle();
        HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(head, content);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, ScreeningExcelRow.class)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("筛选结果")
                .doWrite(rows);

        String fileName = "screening-" + context.taskCode + ".xlsx";
        return new ExportResult(out.toByteArray(), fileName, CONTENT_TYPE);
    }
}
