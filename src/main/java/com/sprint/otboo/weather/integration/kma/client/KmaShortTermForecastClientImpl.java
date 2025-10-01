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
import java.util.LinkedHashMap;
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
                // ▼ 여기서 모든 페이지를 합쳐서 반환
                return fetchAllPages(params);

            } catch (RetryableHttpStatusException e) {
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA retry exceeded for status=" + e.getStatusCode(), e);
                }
                sleepWithJitter(backoffMs, attempts);

            } catch (NonRetryableHttpStatusException e) {
                throw new RuntimeException("KMA non-retryable status=" + e.getStatusCode(), e);

            } catch (SocketTimeoutException e) {
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA request failed after retries", e);
                }
                sleepWithJitter(backoffMs, attempts);

            } catch (IOException e) {
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA request failed after retries", e);
                }
                sleepWithJitter(backoffMs, attempts);
            }
        }
    }

    /** 모든 페이지 순회/병합 */
    private KmaForecastResponse fetchAllPages(Map<String, String> baseParams) throws IOException {
        List<KmaForecastItem> all = new ArrayList<>();

        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int numOfRows = Integer.parseInt(baseParams.getOrDefault("numOfRows", "1000"));

        while (all.size() < totalCount) {
            Map<String, String> p = new LinkedHashMap<>(baseParams);
            p.put("pageNo", String.valueOf(pageNo));

            Page page = fetchOnePage(p);
            if (page.items.isEmpty()) break;

            if (pageNo == 1) {
                totalCount = page.totalCount > 0 ? page.totalCount : page.items.size();
            }
            all.addAll(page.items);

            // 마지막 페이지 추정(아이템 수가 numOfRows보다 적으면 종료)
            if (page.items.size() < numOfRows) break;

            pageNo++;
            if (pageNo > 50) break; // 안전장치
        }

        KmaForecastResponse merged = new KmaForecastResponse();
        merged.setResultCode("00");
        merged.setItems(all);
        return merged;
    }

    /** 단일 페이지 호출 + totalCount/아이템 파싱 */
    private Page fetchOnePage(Map<String, String> params) throws IOException {
        String url = buildUrlWithParams(props.getBaseUrl(), params);
        String body = httpGet(url, props.getConnectTimeoutMs(), props.getReadTimeoutMs());

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

        JsonNode bodyNode  = root.path("response").path("body");
        int totalCount     = bodyNode.path("totalCount").asInt(-1);

        List<KmaForecastItem> items = new ArrayList<>();
        JsonNode itemArray = bodyNode.path("items").path("item");
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

        return new Page(totalCount, items);
    }

    private static final class Page {
        final int totalCount;
        final List<KmaForecastItem> items;
        Page(int totalCount, List<KmaForecastItem> items) {
            this.totalCount = totalCount;
            this.items = items;
        }
    }

    // ================= HTTP/유틸 =================

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
            con.setInstanceFollowRedirects(false);
            con.setUseCaches(false);
            con.setDoInput(true);

            // 안정화용 헤더
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Accept-Encoding", "identity");
            con.setRequestProperty("Connection", "close");
            con.setRequestProperty("User-Agent", "otboo-weather/1.0");

            log.debug("[KMA] GET {}", urlString);
            int code = con.getResponseCode();
            String contentType = con.getHeaderField("Content-Type");

            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            if (is == null) throw new IOException("Empty response stream (code=" + code + ")");
            String body = readFully(is);

            log.debug("[KMA] resp code={} ctype={} len={} head={}", code, contentType, body.length(), head(body));

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
        long jitter = Math.round(base * 0.2 * (Math.random() - 0.5) * 2);
        sleep(base + jitter);
    }

    private String head(String s) {
        if (s == null) return "";
        int n = Math.min(160, s.length());
        return s.substring(0, n).replaceAll("\\s+", " ");
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
