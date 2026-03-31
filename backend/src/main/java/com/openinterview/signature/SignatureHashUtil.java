package com.openinterview.signature;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SignatureHashUtil {
    private SignatureHashUtil() {
    }

    public static String sha256Hex(String raw) {
        if (raw == null) {
            raw = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.SIGNATURE_VERIFY_FAILED, "SIG_HASH", "计算签名哈希失败");
        }
    }
}

