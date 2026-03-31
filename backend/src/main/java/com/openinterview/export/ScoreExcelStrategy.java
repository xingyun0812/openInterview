package com.openinterview.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScoreExcelStrategy implements ExportStrategy {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public ExportResult export(ExportContext context) {
        List<Long> interviewIds = ExportContentParser.parseIds(context.exportContent);
        List<ScoreExcelRow> rows = new ArrayList<>();
        for (Long interviewId : interviewIds) {
            double score = 60.0 + (interviewId % 40);
            rows.add(new ScoreExcelRow(
                    interviewId,
                    "候选人" + interviewId,
                    BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                    interviewId % 3 == 0 ? "待定" : "通过",
                    "综合评价(mock)"
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
        EasyExcel.write(out, ScoreExcelRow.class)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("面试成绩")
                .doWrite(rows);

        String fileName = "scores-" + context.taskCode + ".xlsx";
        return new ExportResult(out.toByteArray(), fileName, CONTENT_TYPE);
    }
}
