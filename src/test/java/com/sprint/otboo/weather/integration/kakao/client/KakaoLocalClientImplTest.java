package com.sprint.otboo.weather.integration.kakao.client;

import static org.assertj.core.api.Assertions.*;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("KakaoLocalClientImpl 테스트")
class KakaoLocalClientImplTest {

    static MockWebServer server;

    @BeforeAll
    static void init() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        server.shutdown();
    }

    private KakaoLocalClient client() {
        String baseUrl = server.url("/").toString();
        WebClient wc = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "KakaoAK test-key")
            .build();
        return new KakaoLocalClientImpl(wc);
    }

    @Test
    void 성공_요청은_JSON을_파싱한다() {
        String body = """
            {"meta":{"total_count":1},"documents":[
              {"region_type":"B","address_name":"서울특별시 중구 태평로1가",
               "region_1depth_name":"서울특별시","region_2depth_name":"중구",
               "region_3depth_name":"태평로1가","region_4depth_name":"",
               "code":"111", "x":126.978, "y":37.5665}
            ]}
            """;
        server.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type","application/json")
            .setBody(body));

        KakaoLocalClient client = client();
        KakaoCoord2RegioncodeResponse res = client.coord2RegionCode(126.9780, 37.5665);

        assertThat(res).isNotNull();
        assertThat(res.documents()).hasSize(1);
        assertThat(res.documents().get(0).region_type()).isEqualTo("B");
    }

    @Test
    void TooManyRequests는_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(429));
        KakaoLocalClient client = client();

        assertThatThrownBy(() -> client.coord2RegionCode(126.9, 37.5))
            .isInstanceOf(WeatherProviderException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEATHER_RATE_LIMIT);
    }

    @Test
    void BadGateway는_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(502));
        KakaoLocalClient client = client();

        assertThatThrownBy(() -> client.coord2RegionCode(126.9, 37.5))
            .isInstanceOf(WeatherProviderException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEATHER_PROVIDER_ERROR);
    }

    @Test
    void GatewayTimeout은_도메인_예외로_매핑된다() {
        server.enqueue(new MockResponse().setResponseCode(504));
        KakaoLocalClient client = client();

        assertThatThrownBy(() -> client.coord2RegionCode(126.9, 37.5))
            .isInstanceOf(WeatherProviderException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WEATHER_TIMEOUT);
    }
}