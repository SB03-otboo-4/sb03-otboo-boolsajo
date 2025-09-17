package com.sprint.otboo.weather.integration.kakao.client;

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
            .build()
            .toUriString();

        return kakao.get()
            .uri(uri)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, ClientResponse::createException)
            .onStatus(HttpStatusCode::is5xxServerError, ClientResponse::createException)
            .bodyToMono(KakaoCoord2RegioncodeResponse.class)
            .block();
    }
}
