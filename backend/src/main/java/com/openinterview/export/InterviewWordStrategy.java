package com.openinterview.export;

import com.deepoove.poi.XWPFTemplate;
import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InterviewWordStrategy implements ExportStrategy {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public ExportResult export(ExportContext context) {
        List<Long> interviewIds = ExportContentParser.parseIds(context.exportContent);
        Long first = interviewIds.get(0);
        String interviewCode = "INT-" + first;
        String candidateName = "候选人" + first;
        String applyPosition = (context.jobCode != null && !context.jobCode.isBlank())
                ? context.jobCode
                : "未指定";

        StringBuilder questions = new StringBuilder();
        StringBuilder scores = new StringBuilder();
        for (Long interviewId : interviewIds) {
            double rating = 75.0 + (interviewId % 25);
            questions.append("问题：请设计一个高并发缓存系统\n")
                    .append("回答：候选人阐述了分层缓存与一致性策略(mock)\n\n");
            scores.append("面试ID ").append(interviewId)
                    .append(" 综合评分：").append(String.format("%.1f", rating)).append("\n");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("interviewCode", interviewCode);
        data.put("candidateName", candidateName);
        data.put("applyPosition", applyPosition);
        data.put("questionsBlock", questions.toString().trim());
        data.put("scoreBlock", scores.toString().trim());

        ClassPathResource resource = new ClassPathResource("templates/interview-report.docx");
        if (!resource.exists()) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_TMPL", "面试报告模板不存在");
        }
        try (InputStream in = resource.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFTemplate template = XWPFTemplate.compile(in).render(data)) {
            template.write(out);
            String fileName = "interview-" + context.taskCode + ".docx";
            return new ExportResult(out.toByteArray(), fileName, CONTENT_TYPE);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_WORD", "生成 Word 失败: " + ex.getMessage());
        }
    }
}
