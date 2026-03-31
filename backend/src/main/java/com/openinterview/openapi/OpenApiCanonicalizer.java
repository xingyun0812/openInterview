package com.openinterview.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OpenApiCanonicalizer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenApiCanonicalizer() {
    }

    public static String canonicalString(String method,
                                         String path,
                                         String rawQuery,
                                         String contentType,
                                         byte[] bodyBytes,
                                         String timestamp,
                                         String nonce) {
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase();
        String normalizedPath = path == null ? "" : path.trim();
        String params = canonicalParams(rawQuery, contentType, bodyBytes);
        return String.join("\n",
                normalizedMethod,
                normalizedPath,
                params,
                timestamp == null ? "" : timestamp.trim(),
                nonce == null ? "" : nonce.trim()
        );
    }

    private static String canonicalParams(String rawQuery, String contentType, byte[] bodyBytes) {
        TreeMap<String, List<String>> kv = new TreeMap<>();
        parseQuery(rawQuery, kv);
        if (bodyBytes != null && bodyBytes.length > 0 && isJson(contentType)) {
            parseJsonBody(bodyBytes, kv);
        }
        return flatten(kv);
    }

    private static boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        return ct.contains("application/json");
    }

    private static void parseQuery(String rawQuery, TreeMap<String, List<String>> out) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return;
        }
        String[] pairs = rawQuery.split("&");
        for (String p : pairs) {
            if (p.isBlank()) {
                continue;
            }
            int idx = p.indexOf('=');
            String k = idx >= 0 ? p.substring(0, idx) : p;
            String v = idx >= 0 ? p.substring(idx + 1) : "";
            String key = urlDecode(k);
            String val = urlDecode(v);
            out.computeIfAbsent(key, kk -> new ArrayList<>()).add(val);
        }
    }

    private static void parseJsonBody(byte[] bodyBytes, TreeMap<String, List<String>> out) {
        try {
            Map<String, Object> obj = MAPPER.readValue(bodyBytes, new TypeReference<Map<String, Object>>() {
            });
            for (Map.Entry<String, Object> e : obj.entrySet()) {
                out.computeIfAbsent(e.getKey(), kk -> new ArrayList<>()).add(stringify(e.getValue()));
            }
        } catch (Exception ignored) {
            // body 不是 JSON object 时，忽略（最小可用策略）
        }
    }

    private static String flatten(TreeMap<String, List<String>> kv) {
        if (kv.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : kv.entrySet()) {
            List<String> vals = e.getValue() == null ? List.of() : e.getValue();
            List<String> sorted = new ArrayList<>(vals);
            sorted.sort(Comparator.naturalOrder());
            if (sorted.isEmpty()) {
                pairs.add(e.getKey() + "=");
            } else {
                for (String v : sorted) {
                    pairs.add(e.getKey() + "=" + (v == null ? "" : v));
                }
            }
        }
        return String.join("&", pairs);
    }

    private static String urlDecode(String s) {
        if (s == null) {
            return "";
        }
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String stringify(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof String) {
            return (String) v;
        }
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        try {
            return MAPPER.writeValueAsString(v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }
}

