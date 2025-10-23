package com.sprint.otboo.weather.integration.owm;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "weather.owm")
public record WeatherOwmProperties(
    String baseUrl,              // ex) https://api.openweathermap.org/data/2.5/forecast
    @NotBlank String apiKey,
    @Min(500) @Max(30000) int connectTimeoutMs,
    @Min(500) @Max(60000) int readTimeoutMs,
    @Min(1) @Max(10) int retryMaxAttempts,
    @Min(0) @Max(10000) long retryBackoffMs,
    String units,                // standard | metric | imperial
    String lang,                 // ko-KR, en, etc.
    boolean probabilityPercent,  // pop을 %로 내릴지 여부 (명세가 %면 true)
    boolean enabled              // 토글: owm 사용 여부
) {

    public WeatherOwmProperties {
        baseUrl = (baseUrl == null || baseUrl.isBlank())
            ? "https://api.openweathermap.org/data/2.5/forecast"
            : baseUrl;

        // 기본값 보강
        connectTimeoutMs = (connectTimeoutMs == 0) ? 3000 : connectTimeoutMs;
        readTimeoutMs = (readTimeoutMs == 0) ? 5000 : readTimeoutMs;
        retryMaxAttempts = (retryMaxAttempts == 0) ? 3 : retryMaxAttempts;
        retryBackoffMs = (retryBackoffMs == 0) ? 300 : retryBackoffMs;
        units = (units == null || units.isBlank()) ? "metric" : units; // 섭씨 권장
        lang = (lang == null || lang.isBlank()) ? "ko-KR" : lang;
    }

    @AssertTrue(message = "apiKey must be present when weather.owm.enabled=true")
    public boolean apiKeyRequiredWhenEnabled() {
        return !enabled || (apiKey != null && !apiKey.isBlank());
    }
}
