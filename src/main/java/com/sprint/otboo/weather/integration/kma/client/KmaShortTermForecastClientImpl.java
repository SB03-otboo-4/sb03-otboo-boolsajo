package com.sprint.otboo.weather.integration.kma.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.weather.integration.kma.WeatherKmaProperties;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KmaShortTermForecastClientImpl implements KmaShortTermForecastClient {

    private final WeatherKmaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KmaShortTermForecastClientImpl(WeatherKmaProperties props) {
        this.props = props;
    }

    @Override
    public KmaForecastResponse getVilageFcst(Map<String, String> params) {
        int attempts = 0;
        int maxAttempts = Math.max(1, props.getRetryMaxAttempts());
        long backoffMs = Math.max(0L, props.getRetryBackoffMs());

        while (true) {
            attempts++;
            try {
                String url = buildUrlWithParams(props.getBaseUrl(), params);
                String body = httpGet(url, props.getConnectTimeoutMs(), props.getReadTimeoutMs());
                return parseResponse(body);

            } catch (RetryableHttpStatusException e) {
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA retry exceeded for status=" + e.getStatusCode(), e);
                }
                sleepWithJitter(backoffMs, attempts);

            } catch (NonRetryableHttpStatusException e) {
                throw new RuntimeException("KMA non-retryable status=" + e.getStatusCode(), e);

            } catch (SocketTimeoutException e) { // 읽기/연결 타임아웃 명시 처리
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA request failed after retries", e);
                }
                sleepWithJitter(backoffMs, attempts);

            } catch (IOException e) { // 기타 I/O 예외
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA request failed after retries", e);
                }
                sleepWithJitter(backoffMs, attempts);
            }
        }
    }

    private String buildUrlWithParams(String baseUrl, Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl).append("?");

        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(encode(e.getKey())).append("=").append(encode(e.getValue()));
            first = false;
        }

        // apihub 전용: 항상 authKey
        String keyValue = props.getAuthKey();
        if (keyValue != null && !keyValue.isEmpty()) {
            if (!first) sb.append("&");
            sb.append("authKey").append("=").append(encodeMaybe(keyValue));
        }
        return sb.toString();
    }

    private String httpGet(String urlString, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        HttpURLConnection con = null;
        try {
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setConnectTimeout(connectTimeoutMs);
            con.setReadTimeout(readTimeoutMs);
            con.setInstanceFollowRedirects(false);    // 불필요 리다이렉트 방지
            con.setUseCaches(false);                   // 캐시 비활성화
            con.setDoInput(true);

            // 안정화용 헤더
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Accept-Encoding", "identity"); // GZIP 비활성화
            con.setRequestProperty("Connection", "close");         // 커넥션 재사용 비활성화
            con.setRequestProperty("User-Agent", "otboo-weather/1.0");

            log.debug("[KMA] GET {}", urlString);
            int code = con.getResponseCode();
            String contentType = con.getHeaderField("Content-Type");

            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            if (is == null) throw new IOException("Empty response stream (code=" + code + ")");
            String body = readFully(is);

            log.debug("[KMA] resp code={} ctype={} len={} head={}", code, contentType, body.length(), head(body));

            // 점검/에러 페이지 등 비-JSON 응답은 서버 상태 문제로 보고 재시도
            String ctype = contentType == null ? "" : contentType.toLowerCase();
            if (!ctype.contains("json")) {
                throw new RetryableHttpStatusException(code == 0 ? 503 : code);
            }

            if (code == 429 || code >= 500) {
                throw new RetryableHttpStatusException(code);
            }
            if (code >= 400) {
                throw new NonRetryableHttpStatusException(code, head(body));
            }

            return body;
        } finally {
            if (con != null) {
                try { con.disconnect(); } catch (Exception ignore) {}
            }
        }
    }

    private String readFully(InputStream is) throws IOException {
        try (BufferedInputStream bin = new BufferedInputStream(is);
            ByteArrayOutputStream bout = new ByteArrayOutputStream(4096)) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = bin.read(buf)) != -1) {
                bout.write(buf, 0, r);
            }
            return bout.toString(StandardCharsets.UTF_8);
        }
    }

    private String encode(String s) throws IOException {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    }

    private String encodeMaybe(String s) throws IOException {
        // 이미 인코딩되어 있으면 그대로 사용
        if (s.contains("%")) return s;
        return encode(s);
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // 간단한 지터(backoff * attempts ± 20%)
    private void sleepWithJitter(long baseBackoffMs, int attempts) {
        long base = Math.max(0L, baseBackoffMs) * Math.max(1, attempts);
        long jitter = Math.round(base * 0.2 * (Math.random() - 0.5) * 2); // ±20%
        sleep(base + jitter);
    }

    private String head(String s) {
        if (s == null) return "";
        int n = Math.min(160, s.length());
        return s.substring(0, n).replaceAll("\\s+", " ");
    }

    private KmaForecastResponse parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);

        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText(null);
        String resultMsg  = header.path("resultMsg").asText("");

        if (resultCode == null || resultCode.isBlank()) {
            throw new IOException("Invalid response (no resultCode). bodyHead=" + head(body));
        }
        if (!"00".equals(resultCode)) {
            throw new IOException("KMA error resultCode=" + resultCode + " msg=" + resultMsg);
        }

        List<KmaForecastItem> items = new ArrayList<>();
        JsonNode itemArray = root.path("response").path("body").path("items").path("item");

        if (itemArray.isArray()) {
            for (JsonNode n : itemArray) {
                KmaForecastItem it = new KmaForecastItem();
                it.setCategory(n.path("category").asText(null));
                it.setFcstDate(n.path("fcstDate").asText(null));
                it.setFcstTime(n.path("fcstTime").asText(null));
                it.setFcstValue(n.path("fcstValue").asText(null));
                items.add(it);
            }
        }

        KmaForecastResponse resp = new KmaForecastResponse();
        resp.setResultCode(resultCode);
        resp.setItems(items);
        return resp;
    }

    private static class RetryableHttpStatusException extends IOException {
        private final int statusCode;
        RetryableHttpStatusException(int statusCode) {
            super("Retryable status: " + statusCode);
            this.statusCode = statusCode;
        }
        int getStatusCode() { return statusCode; }
    }

    private static class NonRetryableHttpStatusException extends IOException {
        private final int statusCode;
        NonRetryableHttpStatusException(int statusCode, String head) {
            super("Non-retryable status: " + statusCode + " bodyHead=" + head);
            this.statusCode = statusCode;
        }
        int getStatusCode() { return statusCode; }
    }
}
