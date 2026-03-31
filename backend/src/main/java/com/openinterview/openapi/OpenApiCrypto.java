package com.openinterview.openapi;

import com.openinterview.common.ApiException;
import com.openinterview.common.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class OpenApiCrypto {
    private OpenApiCrypto() {
    }

    public static String hmacSha256Hex(String secret, String data) {
        if (secret == null) {
            secret = "";
        }
        if (data == null) {
            data = "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.SYSTEM_ERROR, "OPEN_API_CRYPTO", "签名计算失败");
        }
    }
}

