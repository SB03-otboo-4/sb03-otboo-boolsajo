package com.sprint.otboo.weather.integration.kakao.client;

import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

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
            // 429, 502, 504는 도메인 예외로 매핑
            .onStatus(s -> s.value() == 429, rsp -> reactor.core.publisher.Mono.error(
                new com.sprint.otboo.common.exception.weather.WeatherProviderException(
                    com.sprint.otboo.common.exception.ErrorCode.WEATHER_RATE_LIMIT)))
            .onStatus(s -> s.value() == 502, rsp -> reactor.core.publisher.Mono.error(
                new com.sprint.otboo.common.exception.weather.WeatherProviderException(
                    com.sprint.otboo.common.exception.ErrorCode.WEATHER_PROVIDER_ERROR)))
            .onStatus(s -> s.value() == 504, rsp -> reactor.core.publisher.Mono.error(
                new com.sprint.otboo.common.exception.weather.WeatherProviderException(
                    com.sprint.otboo.common.exception.ErrorCode.WEATHER_TIMEOUT)))
            // 그 외 4xx/5xx → 기본 예외
            .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError, rsp -> rsp.createException())
            .onStatus(org.springframework.http.HttpStatusCode::is5xxServerError, rsp -> rsp.createException())
            .bodyToMono(KakaoCoord2RegioncodeResponse.class)
            .block();
    }
}
