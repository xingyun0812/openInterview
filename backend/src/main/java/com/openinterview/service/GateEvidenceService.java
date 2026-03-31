package com.openinterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 聚合 gate4/5/6 证据，判定与 {@code policies/release-gate.yml} 一致。
 */
@Service
public class GateEvidenceService {
    private final EvidenceStore evidenceStore;
    private final EventMappingService eventMappingService;
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper;

    private volatile ReleaseGatePolicy policy = ReleaseGatePolicy.defaults();

    public GateEvidenceService(EvidenceStore evidenceStore,
                               EventMappingService eventMappingService,
                               AuditTrailService auditTrailService,
                               ObjectMapper objectMapper) {
        this.evidenceStore = evidenceStore;
        this.eventMappingService = eventMappingService;
        this.auditTrailService = auditTrailService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadPolicy() {
        try (InputStream in = new ClassPathResource("policies/release-gate.yml").getInputStream()) {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> root = yaml.load(in);
            this.policy = ReleaseGatePolicy.fromYaml(root);
        } catch (Exception ex) {
            this.policy = ReleaseGatePolicy.defaults();
        }
    }

    public Map<String, Object> buildGatePack() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("gate4", buildGate4());
        root.put("gate5", buildGate5());
        root.put("gate6", buildGate6());
        return root;
    }

    public Map<String, Object> buildGateCheck(Map<String, Object> gatePack) {
        @SuppressWarnings("unchecked")
        Map<String, Object> g4 = (Map<String, Object>) gatePack.get("gate4");
        @SuppressWarnings("unchecked")
        Map<String, Object> g5 = (Map<String, Object>) gatePack.get("gate5");
        @SuppressWarnings("unchecked")
        Map<String, Object> g6 = (Map<String, Object>) gatePack.get("gate6");

        List<String> failed = new ArrayList<>();
        if (!"pass".equals(g4.get("status"))) {
            failed.add("gate4");
        }
        if (!"pass".equals(g5.get("status"))) {
            failed.add("gate5");
        }
        if (!"pass".equals(g6.get("status"))) {
            failed.add("gate6");
        }

        Map<String, Object> check = new LinkedHashMap<>();
        if (failed.isEmpty()) {
            check.put("status", "RELEASE_READY");
            check.put("failedGates", List.of());
        } else {
            check.put("status", "BLOCKED");
            check.put("failedGates", failed);
        }
        check.put("gatePack", gatePack);
        return check;
    }

    public void persistGateEvidenceFiles(Map<String, Object> gatePack) throws Exception {
        Path base = Path.of(System.getProperty("user.dir")).resolve("evidence");
        Files.createDirectories(base.resolve("gate4-regression"));
        Files.createDirectories(base.resolve("gate5-event-mapping"));
        Files.createDirectories(base.resolve("gate6-trace-audit"));

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(base.resolve("gate4-regression/gate-pack.json").toFile(), gatePack.get("gate4"));
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(base.resolve("gate5-event-mapping/gate-pack.json").toFile(), gatePack.get("gate5"));
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(base.resolve("gate6-trace-audit/gate-pack.json").toFile(), gatePack.get("gate6"));
    }

    private Map<String, Object> buildGate4() {
        Map<String, Object> block = new LinkedHashMap<>();
        RegressionStats stats = resolveRegressionStats();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("totalTests", stats.totalTests);
        detail.put("passedTests", stats.passedTests);
        detail.put("failureCount", stats.failureCount);
        detail.put("failures", stats.failureNames);
        detail.put("source", stats.source);

        boolean pass = stats.failureCount <= policy.gate4MaxFailures()
                && stats.totalTests > 0;
        block.put("status", pass ? "pass" : "fail");
        block.put("regression", detail);
        return block;
    }

    private RegressionStats resolveRegressionStats() {
        EvidenceStore.RegressionSnapshot snap = evidenceStore.getRegressionSnapshot();
        if (snap != null) {
            int passed = Math.max(0, snap.totalTests - snap.failureCount);
            return new RegressionStats(
                    snap.totalTests,
                    passed,
                    snap.failureCount,
                    snap.failureNames == null ? List.of() : snap.failureNames,
                    "evidence_store_snapshot");
        }
        SurefireAggregate agg = parseSurefireReports(resolveSurefireDir());
        if (agg != null) {
            return new RegressionStats(
                    agg.tests,
                    agg.tests - agg.failures - agg.errors,
                    agg.failures + agg.errors,
                    agg.failureNames,
                    "surefire_reports");
        }
        return new RegressionStats(0, 0, 0, List.of(), "none");
    }

    private Path resolveSurefireDir() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path a = cwd.resolve("target/surefire-reports");
        if (Files.isDirectory(a)) {
            return a;
        }
        Path b = cwd.resolve("backend/target/surefire-reports");
        if (Files.isDirectory(b)) {
            return b;
        }
        return a;
    }

    private SurefireAggregate parseSurefireReports(Path dir) {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> xmls = stream
                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .toList();
            if (xmls.isEmpty()) {
                return null;
            }
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            int tests = 0;
            int failures = 0;
            int errors = 0;
            List<String> failureNames = new ArrayList<>();
            for (Path xml : xmls) {
                try (InputStream in = Files.newInputStream(xml)) {
                    Document doc = f.newDocumentBuilder().parse(in);
                    Element suite = doc.getDocumentElement();
                    if (suite == null || !"testsuite".equals(suite.getTagName())) {
                        continue;
                    }
                    tests += parseIntAttr(suite, "tests");
                    failures += parseIntAttr(suite, "failures");
                    errors += parseIntAttr(suite, "errors");
                    failureNames.addAll(collectFailedCaseNames(suite));
                }
            }
            if (tests == 0 && failures == 0 && errors == 0) {
                return null;
            }
            return new SurefireAggregate(tests, failures, errors, failureNames);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int parseIntAttr(Element el, String name) {
        String v = el.getAttribute(name);
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private List<String> collectFailedCaseNames(Element testsuite) {
        List<String> names = new ArrayList<>();
        NodeList cases = testsuite.getElementsByTagName("testcase");
        for (int i = 0; i < cases.getLength(); i++) {
            if (!(cases.item(i) instanceof Element tc)) {
                continue;
            }
            boolean bad = tc.getElementsByTagName("failure").getLength() > 0
                    || tc.getElementsByTagName("error").getLength() > 0;
            if (bad) {
                String cn = tc.getAttribute("classname");
                String n = tc.getAttribute("name");
                names.add((cn == null || cn.isBlank() ? "?" : cn) + "#" + (n == null ? "?" : n));
            }
        }
        return names;
    }

    private Map<String, Object> buildGate5() {
        Map<String, Object> block = new LinkedHashMap<>();
        Set<String> expected = eventMappingService.expectedMqEventCodes();
        Map<String, String> table = eventMappingService.mappingTable();

        Set<String> mqCodes = new HashSet<>();
        for (EventMessage e : evidenceStore.getMqEvents()) {
            if (e.eventCode != null) {
                mqCodes.add(e.eventCode);
            }
        }
        Set<String> webhookCodes = new HashSet<>();
        for (EventMessage e : evidenceStore.getWebhookEvents()) {
            if (e.eventCode != null) {
                webhookCodes.add(e.eventCode);
            }
        }

        List<String> missing = new ArrayList<>();
        int covered = 0;
        for (String mq : expected) {
            String wh = table.getOrDefault(mq, mq);
            boolean ok = mqCodes.contains(mq) && webhookCodes.contains(wh);
            if (ok) {
                covered++;
            } else {
                missing.add(mq + " -> " + wh);
            }
        }

        double ratio = expected.isEmpty() ? 1.0 : (double) covered / expected.size();
        boolean pass = ratio + 1e-9 >= policy.gate5MinCoverage() && missing.isEmpty();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("expectedEventCount", expected.size());
        detail.put("coveredCount", covered);
        detail.put("coverageRatio", ratio);
        detail.put("missingEvents", missing);

        block.put("status", pass ? "pass" : "fail");
        block.put("eventMapping", detail);
        return block;
    }

    private Map<String, Object> buildGate6() {
        Map<String, Object> block = new LinkedHashMap<>();
        List<AuditTrailService.AuditRecord> list = auditTrailService.list();
        int n = list.size();
        int traceHits = 0;
        int bizHits = 0;
        int errorHits = 0;
        List<Map<String, String>> samples = new ArrayList<>();

        for (AuditTrailService.AuditRecord r : list) {
            boolean t = r.traceId != null && !r.traceId.isBlank();
            boolean b = r.bizCode != null && !r.bizCode.isBlank();
            boolean e = r.errorCode != null && !r.errorCode.isBlank();
            if (t) {
                traceHits++;
            }
            if (b) {
                bizHits++;
            }
            if (e) {
                errorHits++;
            }
            if (!t || !b || !e) {
                if (samples.size() < 8) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("module", r.module);
                    row.put("action", r.action);
                    row.put("traceId", r.traceId);
                    row.put("bizCode", r.bizCode);
                    row.put("errorCode", r.errorCode);
                    samples.add(row);
                }
            }
        }

        double traceRate = n == 0 ? 1.0 : (double) traceHits / n;
        double bizRate = n == 0 ? 1.0 : (double) bizHits / n;
        double errRate = n == 0 ? 1.0 : (double) errorHits / n;

        boolean pass = n > 0
                && traceRate + 1e-9 >= policy.gate6MinTraceHit()
                && bizRate + 1e-9 >= policy.gate6MinBizHit()
                && errRate + 1e-9 >= policy.gate6MinErrorHit();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("sampleSize", n);
        detail.put("traceIdHitRate", traceRate);
        detail.put("bizCodeHitRate", bizRate);
        detail.put("errorCodeHitRate", errRate);
        detail.put("missingFieldSamples", samples);

        block.put("status", pass ? "pass" : "fail");
        block.put("traceAudit", detail);
        return block;
    }

    private record RegressionStats(int totalTests, int passedTests, int failureCount,
                                   List<String> failureNames, String source) {
    }

    private record SurefireAggregate(int tests, int failures, int errors, List<String> failureNames) {
    }

    private static final class ReleaseGatePolicy {
        private final int gate4MaxFailures;
        private final double gate5MinCoverage;
        private final double gate6MinTrace;
        private final double gate6MinBiz;
        private final double gate6MinError;

        private ReleaseGatePolicy(int gate4MaxFailures, double gate5MinCoverage,
                                  double gate6MinTrace, double gate6MinBiz, double gate6MinError) {
            this.gate4MaxFailures = gate4MaxFailures;
            this.gate5MinCoverage = gate5MinCoverage;
            this.gate6MinTrace = gate6MinTrace;
            this.gate6MinBiz = gate6MinBiz;
            this.gate6MinError = gate6MinError;
        }

        static ReleaseGatePolicy defaults() {
            return new ReleaseGatePolicy(0, 1.0, 1.0, 1.0, 1.0);
        }

        @SuppressWarnings("unchecked")
        static ReleaseGatePolicy fromYaml(Map<String, Object> root) {
            if (root == null) {
                return defaults();
            }
            int g4 = 0;
            double g5 = 1.0;
            double t = 1.0;
            double b = 1.0;
            double e = 1.0;
            Object g4o = root.get("gate4");
            if (g4o instanceof Map<?, ?> m) {
                Object reg = m.get("regression");
                if (reg instanceof Map<?, ?> r) {
                    Object mf = r.get("max_failures");
                    if (mf instanceof Number n) {
                        g4 = n.intValue();
                    }
                }
            }
            Object g5o = root.get("gate5");
            if (g5o instanceof Map<?, ?> m) {
                Object em = m.get("event_mapping");
                if (em instanceof Map<?, ?> r) {
                    Object mc = r.get("min_coverage_ratio");
                    if (mc instanceof Number n) {
                        g5 = n.doubleValue();
                    }
                }
            }
            Object g6o = root.get("gate6");
            if (g6o instanceof Map<?, ?> m) {
                Object ta = m.get("trace_audit");
                if (ta instanceof Map<?, ?> r) {
                    Object v = r.get("min_trace_hit_rate");
                    if (v instanceof Number n) {
                        t = n.doubleValue();
                    }
                    v = r.get("min_biz_code_hit_rate");
                    if (v instanceof Number n) {
                        b = n.doubleValue();
                    }
                    v = r.get("min_error_code_hit_rate");
                    if (v instanceof Number n) {
                        e = n.doubleValue();
                    }
                }
            }
            return new ReleaseGatePolicy(g4, g5, t, b, e);
        }

        int gate4MaxFailures() {
            return gate4MaxFailures;
        }

        double gate5MinCoverage() {
            return gate5MinCoverage;
        }

        double gate6MinTraceHit() {
            return gate6MinTrace;
        }

        double gate6MinBizHit() {
            return gate6MinBiz;
        }

        double gate6MinErrorHit() {
            return gate6MinError;
        }
    }
}
