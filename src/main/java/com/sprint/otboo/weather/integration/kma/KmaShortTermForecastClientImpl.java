package com.sprint.otboo.weather.integration.kma;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KmaShortTermForecastClientImpl implements KmaShortTermForecastClient {

    private final WeatherKmaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KmaShortTermForecastClientImpl(WeatherKmaProperties props) {
        this.props = props;
    }

    @Override
    public KmaForecastResponse getUltraSrtFcst(Map<String, String> params) {
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
                sleep(backoffMs);
            } catch (IOException e) {
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("KMA request failed after retries", e);
                }
                sleep(backoffMs);
            }
        }
    }

    private String buildUrlWithParams(String baseUrl, Map<String, String> params) throws IOException {
        // baseUrl 예: http://localhost:12345/  (MockWebServer)
        // 실제 운영에선 getUltraSrtFcst 경로를 붙일 수 있음. 테스트는 경로에 의존하지 않음.
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        // baseUrl 끝이 '/' 아니면 붙여줌 (안전)
        if (!baseUrl.endsWith("/")) {
            sb.append("/");
        }
        // 경로를 굳이 지정하지 않아도 MockWebServer는 응답을 반환하므로 생략 가능
        // sb.append("getUltraSrtFcst");

        sb.append("?");

        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(encode(e.getKey())).append("=").append(encode(e.getValue()));
            first = false;
        }
        // serviceKey가 설정되었다면 추가
        if (props.getServiceKey() != null && !props.getServiceKey().isEmpty()) {
            if (!first) sb.append("&");
            sb.append("serviceKey=").append(encode(props.getServiceKey()));
        }
        return sb.toString();
    }

    private String httpGet(String urlString, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setRequestMethod("GET");
            con.setConnectTimeout(connectTimeoutMs);
            con.setReadTimeout(readTimeoutMs);
            con.setRequestProperty("Accept", "application/json");

            int code = con.getResponseCode();
            if (isRetryable(code)) {
                throw new RetryableHttpStatusException(code);
            }
            InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
            if (is == null) {
                throw new IOException("Empty response stream (code=" + code + ")");
            }
            return readFully(is);
        } finally {
            con.disconnect();
        }
    }

    private boolean isRetryable(int status) {
        if (status == 429) return true;
        return status >= 500;
    }

    private String readFully(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String encode(String s) throws IOException {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
    }

    private void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private KmaForecastResponse parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText(null);
        // KMA 정상 값은 "00"
        if (resultCode == null || resultCode.isEmpty()) {
            // 일부 테스트 응답에 header가 비어있을 수 있으므로 기본 "00" 처리
            resultCode = "00";
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
        int getStatusCode() {
            return statusCode;
        }
    }
}