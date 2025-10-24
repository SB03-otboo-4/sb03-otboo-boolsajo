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
        String code = (locale == null) ? defaultLang : locale.toLanguageTag();
        if (code == null || code.isBlank()) return "en";
        if (code.equalsIgnoreCase("ko-KR")) return "kr";
        if (code.equalsIgnoreCase("ko"))    return "kr";
        // 2글자만 사용
        return code.length() > 2 ? code.substring(0, 2).toLowerCase() : code.toLowerCase();
    }
}
