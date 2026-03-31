package com.openinterview.export;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;

import java.util.ArrayList;
import java.util.List;

final class ExportContentParser {
    private ExportContentParser() {
    }

    static List<Long> parseIds(String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "ID 列表不能为空");
        }
        List<Long> ids = new ArrayList<>();
        for (String part : content.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            ids.add(Long.parseLong(t));
        }
        if (ids.isEmpty()) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "EXP_INVALID", "ID 列表不能为空");
        }
        return ids;
    }
}
