package com.sprint.otboo.weather.integration.kakao.client;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.weather.WeatherProviderException;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import java.net.ConnectException;
import java.time.Duration;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

@Component
public class KakaoLocalClientImpl implements KakaoLocalClient {

    private final WebClient kakao;

    public KakaoLocalClientImpl(
        @Qualifier("owmWebClient") WebClient kakao) {
        this.kakao = kakao;
    }

    @Override
    public KakaoCoord2RegioncodeResponse coord2RegionCode(double longitude, double latitude) {
        String uri = UriComponentsBuilder.fromPath("/v2/local/geo/coord2regioncode.json")
            .queryParam("x", longitude)
            .queryParam("y", latitude)
            .toUriString();

        // 429만 즉시 매핑(재시도 X)
        Predicate<HttpStatusCode> is429 =
            status -> status != null && status.value() == HttpStatus.TOO_MANY_REQUESTS.value();

        return kakao.get()
            .uri(uri)
            .retrieve()
            // 429는 즉시 RATE_LIMIT, 504는 즉시 TIMEOUT (재시도 X)
            .onStatus(
                s -> s != null && s.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
                r -> Mono.error(new WeatherProviderException(ErrorCode.WEATHER_RATE_LIMIT))
            )
            .onStatus(
                s -> s != null && s.value() == HttpStatus.GATEWAY_TIMEOUT.value(),
                r -> Mono.error(new WeatherProviderException(ErrorCode.WEATHER_TIMEOUT))
            )
            .bodyToMono(KakaoCoord2RegioncodeResponse.class)

            .retryWhen(
                Retry.fixedDelay(2, Duration.ofMillis(50))
                    .filter(t -> {
                        Throwable root = Exceptions.unwrap(t); // 언랩해서 판정
                        return (root instanceof WebClientRequestException)
                            || (root instanceof AbortedException)
                            || (root instanceof PrematureCloseException)
                            || (root != null && root.getCause() instanceof ConnectException)
                            || (root instanceof WebClientResponseException we
                            && we.getStatusCode().value() == 502); // 502만 재시도 대상으로
                    })
            )

            // 최종 안전망: 상태코드→도메인 예외 매핑
            .onErrorMap(t -> {
                if (t instanceof WeatherProviderException) return t;

                // 재시도 소진: root 기준으로 상태코드 매핑
                if (Exceptions.isRetryExhausted(t)) {
                    Throwable root = Exceptions.unwrap(t);
                    if (root instanceof WebClientResponseException wex) {
                        int code = wex.getStatusCode().value();
                        if (code == 429) return new WeatherProviderException(ErrorCode.WEATHER_RATE_LIMIT);
                        if (code == 502) return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                        if (code == 504) return new WeatherProviderException(ErrorCode.WEATHER_TIMEOUT);
                        return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                    }
                    if (root instanceof WebClientRequestException
                        || root instanceof AbortedException
                        || root instanceof PrematureCloseException
                        || (root != null && root.getCause() instanceof ConnectException)) {
                        return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                    }
                    return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                }

                // 재시도 소진이 아닌 즉시 발생 HTTP 오류
                if (t instanceof WebClientResponseException wex) {
                    int code = wex.getStatusCode().value();
                    if (code == 429) return new WeatherProviderException(ErrorCode.WEATHER_RATE_LIMIT);
                    if (code == 502) return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                    if (code == 504) return new WeatherProviderException(ErrorCode.WEATHER_TIMEOUT);
                    return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                }

                // 일반 네트워크/전송 계열
                if (t instanceof WebClientRequestException
                    || t instanceof AbortedException
                    || t instanceof PrematureCloseException
                    || (t != null && t.getCause() instanceof ConnectException)) {
                    return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
                }
                return t;
            })
            .block();
    }
}
