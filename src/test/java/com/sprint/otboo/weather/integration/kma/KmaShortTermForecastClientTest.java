package com.sprint.otboo.weather.integration.kma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClientImpl;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RED: 아직 KmaShortTermForecastClientImpl 미구현이므로 실패해야 정상.
 *  - 정상 응답 파싱
 *  - 카테고리 누락 허용(존재하는 항목만 반환)
 *  - 429/5xx 재시도(backoff 포함)
 *  - 타임아웃 처리
 */
@DisplayName("KmaShortTermForecastClient 테스트")
class KmaShortTermForecastClientTest {

    private MockWebServer server;
    private WeatherKmaProperties props;
    private KmaRequestBuilder builder;
    private KmaShortTermForecastClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        props = new WeatherKmaProperties();
        props.setBaseUrl(server.url("/").toString());
        props.setConnectTimeoutMs(1000);
        props.setReadTimeoutMs(1000);
        props.setRetryMaxAttempts(3);
        props.setRetryBackoffMs(10);
        props.setNumOfRows(1000);
        props.setDataType("JSON");

        builder = new KmaRequestBuilder(props);
        client = new KmaShortTermForecastClientImpl(props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("정상 응답을 파싱하여 카테고리별 항목을 반환해야 한다")
    void 정상_응답_파싱() throws Exception {
        String body = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
            "body": {
              "dataType": "JSON",
              "items": {
                "item": [
                  { "category": "TMP", "fcstDate": "20250924", "fcstTime": "1100", "fcstValue": "24" },
                  { "category": "PTY", "fcstDate": "20250924", "fcstTime": "1100", "fcstValue": "0" },
                  { "category": "POP", "fcstDate": "20250924", "fcstTime": "1100", "fcstValue": "30" },
                  { "category": "SKY", "fcstDate": "20250924", "fcstTime": "1100", "fcstValue": "3" }
                ]
              },
              "totalCount": 4
            }
          }
        }
        """;
        server.enqueue(new MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Map<String, String> params = builder.toParams(37.5665, 126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        KmaForecastResponse resp = client.getVilageFcst(params);

        // 계약: resultCode=00이고, 항목이 카테고리별로 파싱되어야 함
        assertThat(resp.getResultCode()).isEqualTo("00");
        List<KmaForecastItem> items = resp.getItems();
        assertThat(items).hasSize(4);
        assertThat(items.stream().anyMatch(i -> "TMP".equals(i.getCategory()))).isTrue();
        assertThat(items.stream().anyMatch(i -> "PTY".equals(i.getCategory()))).isTrue();
        assertThat(items.stream().anyMatch(i -> "POP".equals(i.getCategory()))).isTrue();
        assertThat(items.stream().anyMatch(i -> "SKY".equals(i.getCategory()))).isTrue();
    }

    @Test
    @DisplayName("일부 카테고리 누락 시, 존재하는 항목만 반환해야 한다(에러 없이)")
    void 카테고리_누락_허용() throws Exception {
        String body = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
            "body": {
              "dataType": "JSON",
              "items": {
                "item": [
                  { "category": "TMP", "fcstDate": "20250924", "fcstTime": "1100", "fcstValue": "24" }
                ]
              },
              "totalCount": 1
            }
          }
        }
        """;
        server.enqueue(new MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Map<String, String> params = builder.toParams(37.5665, 126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        KmaForecastResponse resp = client.getVilageFcst(params);

        assertThat(resp.getResultCode()).isEqualTo("00");
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getCategory()).isEqualTo("TMP");
    }

    @Test
    @DisplayName("429 발생 시 설정된 횟수만큼 재시도 후 성공을 반환해야 한다")
    void TooManyRequests_재시도() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(429)); // 1st
        server.enqueue(new MockResponse().setResponseCode(429)); // 2nd
        server.enqueue(new MockResponse() // 3rd - success
            .setBody("""
                { "response": { "header": { "resultCode": "00" }, "body": { "items": { "item": [] }, "totalCount": 0 } } }
                """)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Map<String, String> params = builder.toParams(37.5665, 126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        KmaForecastResponse resp = client.getVilageFcst(params);

        assertThat(resp.getResultCode()).isEqualTo("00");
        assertThat(resp.getItems()).isEmpty();
    }

    @Test
    @DisplayName("5xx 연속 실패 시 예외를 던져야 한다")
    void 서버오류_연속시_예외() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(502));
        server.enqueue(new MockResponse().setResponseCode(503));

        Map<String, String> params = builder.toParams(37.5665, 126.9780, Instant.parse("2025-09-24T10:05:00Z"));

        assertThrows(RuntimeException.class, () -> client.getVilageFcst(params));
    }

    @Test
    @DisplayName("읽기 타임아웃 시 예외를 던져야 한다")
    void 타임아웃_예외() {
        server.enqueue(new MockResponse()
            .setBodyDelay(2, TimeUnit.SECONDS)
            .setBody("{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":[]},\"totalCount\":0}}}")
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

        Map<String, String> params = builder.toParams(37.5665, 126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        assertThrows(RuntimeException.class, () -> client.getVilageFcst(params));
    }

    @Test
    @DisplayName("resultCode가 00이 아니면 IOException으로 실패해야 한다")
    void 결과코드_비정상_예외() {
        server.enqueue(new MockResponse()
            .setBody("{\"response\":{\"header\":{\"resultCode\":\"03\",\"resultMsg\":\"AUTH FAILED\"}}}")
            .addHeader("Content-Type","application/json").setResponseCode(200));

        Map<String,String> params = builder.toParams(37.5665,126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        assertThrows(RuntimeException.class, () -> client.getVilageFcst(params));
    }

    @Test
    @DisplayName("429/5xx 혼합으로 재시도 한계 도달 시 예외를 던져야 한다")
    void 혼합_오류_재시도_한계() {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(502));

        Map<String,String> params = builder.toParams(37.5665,126.9780, Instant.parse("2025-09-24T10:05:00Z"));
        assertThrows(RuntimeException.class, () -> client.getVilageFcst(params));
    }
}
