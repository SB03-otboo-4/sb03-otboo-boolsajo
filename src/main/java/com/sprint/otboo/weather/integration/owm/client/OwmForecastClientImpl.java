package com.sprint.otboo.weather.integration.owm.client;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
public class OwmForecastClientImpl implements OwmForecastClient {

    private final WebClient client;   // @Qualifier("owmWebClient")가 주입됨
    private final String apiKey;
    private final String baseUrl;     // 사용하지 않아도 되지만 주입 받아둠(검증/로그용)
    private final String units;       // ex) "metric"
    private final String defaultLang; // ex) "kr" 또는 "ko-KR"

    @Override
    public OwmForecastResponse get5Day3Hour(double lat, double lon, Locale locale) {
        // baseUrl은 WebClient의 baseUrl에 이미 설정되어 있음: https://api.openweathermap.org/data/2.5
        // 여기서는 상대경로(/forecast)만 붙인다.
        return client.get()
            .uri(uriBuilder -> uriBuilder
                .path("/forecast")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", apiKey)
                .queryParam("units", units)
                .queryParam("lang", normalizeLang(locale))
                .build()
            )
            .retrieve()
            .bodyToMono(OwmForecastResponse.class)
            .block();
    }

    private String normalizeLang(Locale locale) {
        // OWM은 보통 2글자 코드 선호. ko/kr 모두 동작하지만 프로젝트 기본을 'kr'로 통일하는 케이스가 많음.
        String code = (locale == null) ? defaultLang : locale.toLanguageTag();
        if (code == null || code.isBlank()) return "en";
        if (code.equalsIgnoreCase("ko-KR")) return "kr";
        if (code.equalsIgnoreCase("ko"))    return "kr";
        // 2글자만 사용
        return code.length() > 2 ? code.substring(0, 2).toLowerCase() : code.toLowerCase();
    }
}
