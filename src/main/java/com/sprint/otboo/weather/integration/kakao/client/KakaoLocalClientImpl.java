package com.sprint.otboo.weather.integration.kakao.client;

import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
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
            .onStatus(HttpStatusCode::is4xxClientError, resp ->
                resp.createException().flatMap(ex -> {
                    int code = resp.statusCode().value();
                    if (code == 429) { // Too Many Requests
                        return reactor.core.publisher.Mono.error(WeatherProviderException.tooManyRequests());
                    }
                    return reactor.core.publisher.Mono.error(WeatherProviderException.badGateway());
                })
            )
            .onStatus(HttpStatusCode::is5xxServerError, resp ->
                resp.createException().flatMap(ex -> {
                    int code = resp.statusCode().value();
                    if (code == 504) { // Gateway Timeout
                        return reactor.core.publisher.Mono.error(WeatherProviderException.timeout());
                    }
                    return reactor.core.publisher.Mono.error(WeatherProviderException.badGateway());
                })
            )
            .bodyToMono(KakaoCoord2RegioncodeResponse.class)
            .timeout(java.time.Duration.ofSeconds(5)) // 호출 타임아웃
            .block();
    }
}
