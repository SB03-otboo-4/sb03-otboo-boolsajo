package com.sprint.otboo.weather.integration.owm.client;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
public class OwmForecastClientImpl implements OwmForecastClient {

    private final WebClient webClient;
    private final String apiKey;          // yaml 주입
    private final String baseUrl;         // https://api.openweathermap.org/data/2.5/forecast

    @Override
    public OwmForecastResponse get5Day3Hour(double lat, double lon, Locale locale) {
        return webClient.get()
            .uri(uri -> uri.scheme("https")
                .host("api.openweathermap.org")
                .path("/data/2.5/forecast")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", locale == null ? "en" : locale.toLanguageTag())
                .build())
            .retrieve()
            .bodyToMono(OwmForecastResponse.class)
            .block();
    }
}
