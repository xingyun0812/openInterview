import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  const traceId = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const headers = {
    "Content-Type": "application/json",
    "X-Trace-Id": traceId,
    "X-Idempotency-Key": `k6-idem-${__VU}-${__ITER}`,
  };

  const payload = JSON.stringify({
    candidateId: `c-${__VU}`,
    resumeText: "k6 smoke resume text",
  });

  const res = http.post(`${BASE_URL}/api/v1/candidate/resume/screen`, payload, { headers });

  check(res, {
    "status is 200": (r) => r.status === 200,
  });

  sleep(1);
}

