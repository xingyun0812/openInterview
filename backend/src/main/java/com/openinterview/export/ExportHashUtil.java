package com.openinterview.export;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;

import java.security.MessageDigest;

public final class ExportHashUtil {
    private ExportHashUtil() {
    }

    public static String sha256Hex(byte[] raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.EXPORT_FILE_FAILED, "EXP_HASH", "计算文件哈希失败");
        }
    }
}
