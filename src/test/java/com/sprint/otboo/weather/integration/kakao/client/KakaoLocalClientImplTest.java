package com.sprint.otboo.weather.integration.kakao.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("KakaoLocalClientImpl 테스트")
class KakaoLocalClientImplTest {

    static MockWebServer server;
    KakaoLocalClientImpl client;

    @BeforeAll
    static void setupServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void shutdown() throws IOException {
        server.shutdown();
    }

    @BeforeEach
    void setUp() {
        String baseUrl = server.url("/").toString();
        WebClient kakaoWebClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK test-key")
            .build();
        client = new KakaoLocalClientImpl(kakaoWebClient);
    }

    @Test
    void 성공_요청은_JSON을_파싱한다() {
        // 쿼리 매칭: x=126.9780, y=37.5665 (테스트 입력과 동일해야 함)
        server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
              {"documents":[{"address_name":"서울특별시 중구 태평로1가"}]}
            """));

        KakaoCoord2RegioncodeResponse resp = client.coord2RegionCode(126.9780, 37.5665);
        assertThat(resp.documents()).isNotEmpty();
    }

    @Test
    void TooManyRequests는_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(429));
        assertThatThrownBy(() -> client.coord2RegionCode(0,0))
            .isInstanceOf(WeatherProviderException.class)
            .hasMessageContaining(ErrorCode.WEATHER_RATE_LIMIT.name());
    }

    @Test
    void BadGateway는_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(502));
        assertThatThrownBy(() -> client.coord2RegionCode(0,0))
            .isInstanceOf(WeatherProviderException.class)
            .hasMessageContaining(ErrorCode.WEATHER_PROVIDER_ERROR.name());
    }

    @Test
    void GatewayTimeout은_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(504));
        assertThatThrownBy(() -> client.coord2RegionCode(0,0))
            .isInstanceOf(WeatherProviderException.class)
            .hasMessageContaining(ErrorCode.WEATHER_TIMEOUT.name());
    }
}