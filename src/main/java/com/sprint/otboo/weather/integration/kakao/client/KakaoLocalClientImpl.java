package com.sprint.otboo.weather.integration.kakao.client;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class KakaoLocalClientImpl implements KakaoLocalClient {

    private final WebClient kakao;

    public KakaoLocalClientImpl(@Qualifier("kakaoWebClient") WebClient kakao) {
        this.kakao = kakao;
    }

    @Override
    public KakaoCoord2RegioncodeResponse coord2RegionCode(double longitude, double latitude) {
        String uri = UriComponentsBuilder.fromPath("/v2/local/geo/coord2regioncode.json")
            .queryParam("x", longitude)
            .queryParam("y", latitude)
            .toUriString();

        return kakao.get()
            .uri(uri)
            .retrieve()
            // 429, 502, 504 → 도메인 예외 매핑
            .onStatus(s -> s.value() == 429,
                rsp -> Mono.error(new WeatherProviderException(ErrorCode.WEATHER_RATE_LIMIT)))
            .onStatus(s -> s.value() == 502,
                rsp -> Mono.error(new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR)))
            .onStatus(s -> s.value() == 504,
                rsp -> Mono.error(new WeatherProviderException(ErrorCode.WEATHER_TIMEOUT)))
            // 나머지 4xx/5xx → 기본 예외
            .onStatus(HttpStatusCode::is4xxClientError, rsp -> rsp.createException())
            .onStatus(HttpStatusCode::is5xxServerError, rsp -> rsp.createException())
            .bodyToMono(KakaoCoord2RegioncodeResponse.class)
            // 네트워크/응답 타임아웃(최대 대기 3초)
            .timeout(Duration.ofSeconds(3))
            // 429/5xx(서버 오류)만 최대 2회 백오프 재시도
            .retryWhen(
                Retry.backoff(2, Duration.ofMillis(200))
                    .filter(ex ->
                        ex instanceof WeatherProviderException // 우리가 명시 매핑한 429/502/504
                            || (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException we
                            && we.getStatusCode().is5xxServerError()))
            )
            .block();
    }
}
