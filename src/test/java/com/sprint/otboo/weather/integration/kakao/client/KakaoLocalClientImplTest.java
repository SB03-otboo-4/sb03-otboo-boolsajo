package com.sprint.otboo.weather.integration.kakao.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import java.net.ConnectException;
import java.net.URI;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@DisplayName("KakaoLocalClientImpl 테스트")
class KakaoLocalClientImplTest {

    /** 공통 체인 mock 구성 유틸: get → uri → (header/headers/accept) → retrieve → onStatus* → bodyToMono */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static class Chain {
        final WebClient webClient = mock(WebClient.class);
        final RequestHeadersUriSpec<?> uriSpec = (RequestHeadersUriSpec<?>) mock(RequestHeadersUriSpec.class);
        final RequestHeadersSpec<?> headersSpec = (RequestHeadersSpec<?>) mock(RequestHeadersSpec.class);
        final ResponseSpec responseSpec = mock(ResponseSpec.class);

        Chain() {
            // 1) get()
            doReturn(uriSpec).when(webClient).get();

            // 2) uri(String) / uri(Function)
            doReturn(headersSpec).when(uriSpec).uri(anyString());
            doReturn(headersSpec).when(uriSpec).uri((Function) any(Function.class));

            // 3) 체인 메서드: 자기 자신 반환
            doReturn(headersSpec).when(headersSpec).header(anyString(), anyString());
            doReturn(headersSpec).when(headersSpec).headers(any());
            doReturn(headersSpec).when(headersSpec).accept((MediaType[]) any());

            // 4) retrieve()
            doReturn(responseSpec).when(headersSpec).retrieve();

            // 5) onStatus(...) → 반드시 자기 자신 반환하도록!
            doReturn(responseSpec).when(responseSpec).onStatus(
                (Predicate<HttpStatusCode>) any(Predicate.class),
                (Function<ClientResponse, Mono<? extends Throwable>>) any(Function.class)
            );
            // (여러 번 호출되더라도 동일 stub로 자기 자신 반환)
        }
    }

    /** 성공 응답을 반환하는 WebClient */
    private WebClient 성공웹클라이언트(KakaoCoord2RegioncodeResponse ok) {
        Chain c = new Chain();
        doReturn(Mono.just(ok))
            .when(c.responseSpec)
            .bodyToMono(KakaoCoord2RegioncodeResponse.class);
        return c.webClient;
    }

    /** 지정한 상태코드 오류를 던지는 WebClient */
    private WebClient 상태오류웹클라이언트(int httpStatus) {
        Chain c = new Chain();
        WebClientResponseException ex = WebClientResponseException.create(
            httpStatus, "error", null, new byte[0], null
        );
        doReturn(Mono.error(ex))
            .when(c.responseSpec)
            .bodyToMono(KakaoCoord2RegioncodeResponse.class);
        return c.webClient;
    }

    /** 임의 Throwable을 던지는 WebClient */
    private WebClient 예외웹클라이언트(Throwable throwable) {
        Chain c = new Chain();
        doReturn(Mono.error(throwable))
            .when(c.responseSpec)
            .bodyToMono(KakaoCoord2RegioncodeResponse.class);
        return c.webClient;
    }

    // =======================
    // 테스트 케이스
    // =======================

    @Test
    void 성공_요청은_JSON을_파싱한다() {
        // given
        KakaoCoord2RegioncodeResponse ok = mock(KakaoCoord2RegioncodeResponse.class);
        WebClient webClient = 성공웹클라이언트(ok);
        KakaoLocalClientImpl sut = new KakaoLocalClientImpl(webClient);

        // when
        KakaoCoord2RegioncodeResponse res = sut.coord2RegionCode(126.9780, 37.5665);

        // then
        assertNotNull(res);
    }

    @Test
    void 상태코드_429면_RATE_LIMIT_에러코드로_예외를_던진다() {
        // given
        WebClient webClient = 상태오류웹클라이언트(HttpStatus.TOO_MANY_REQUESTS.value());
        KakaoLocalClientImpl sut = new KakaoLocalClientImpl(webClient);

        // when & then
        WeatherProviderException ex =
            assertThrows(WeatherProviderException.class, () -> sut.coord2RegionCode(0.0, 0.0));
        assertEquals(ErrorCode.WEATHER_RATE_LIMIT, ex.getErrorCode());
    }

    @Test
    void 상태코드_502면_PROVIDER_ERROR_에러코드로_예외를_던진다() {
        // given
        WebClient webClient = 상태오류웹클라이언트(HttpStatus.BAD_GATEWAY.value());
        KakaoLocalClientImpl sut = new KakaoLocalClientImpl(webClient);

        // when & then
        WeatherProviderException ex =
            assertThrows(WeatherProviderException.class, () -> sut.coord2RegionCode(0.0, 0.0));
        assertEquals(ErrorCode.WEATHER_PROVIDER_ERROR, ex.getErrorCode());
    }

    @Test
    void 상태코드_504면_TIMEOUT_에러코드로_예외를_던진다() {
        // given
        WebClient webClient = 상태오류웹클라이언트(HttpStatus.GATEWAY_TIMEOUT.value());
        KakaoLocalClientImpl sut = new KakaoLocalClientImpl(webClient);

        // when & then
        WeatherProviderException ex =
            assertThrows(WeatherProviderException.class, () -> sut.coord2RegionCode(0.0, 0.0));
        assertEquals(ErrorCode.WEATHER_TIMEOUT, ex.getErrorCode());
    }

    @Test
    void 네트워크예외면_재시도후_예외를_던진다() {
        // given
        WebClientRequestException cause = new WebClientRequestException(
            new ConnectException("conn"),
            HttpMethod.GET,
            URI.create("http://example.local/test"),
            HttpHeaders.EMPTY
        );
        WebClient webClient = 예외웹클라이언트(cause);
        KakaoLocalClientImpl sut = new KakaoLocalClientImpl(webClient);

        // when & then
        assertThrows(WeatherProviderException.class, () -> sut.coord2RegionCode(127.0, 37.0));
    }
}
