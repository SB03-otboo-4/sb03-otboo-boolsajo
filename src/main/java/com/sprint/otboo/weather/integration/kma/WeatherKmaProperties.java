package com.sprint.otboo.weather.integration.kma;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "weather.kma")
public record WeatherKmaProperties(
    String baseUrl,              // 공통 root: https://apihub.kma.go.kr/api/typ02/openApi
    String vilageFcstPath,       // 단기예보 경로
    String authKey,
    @Min(500) @Max(30000) int connectTimeoutMs,
    @Min(500) @Max(60000) int readTimeoutMs,
    @Min(1) @Max(10) int retryMaxAttempts,
    @Min(0) @Max(10000) long retryBackoffMs,
    @Min(10) @Max(1000) int numOfRows,
    String dataType,
    boolean enabled
) {

    public WeatherKmaProperties {
        // 기본값 보강
        baseUrl = (baseUrl == null || baseUrl.isBlank())
            ? "https://apihub.kma.go.kr/api/typ02/openApi"
            : baseUrl;

        vilageFcstPath = (vilageFcstPath == null || vilageFcstPath.isBlank())
            ? "/VilageFcstInfoService_2.0/getVilageFcst"
            : vilageFcstPath;

        dataType = (dataType == null || dataType.isBlank()) ? "JSON" : dataType;
        connectTimeoutMs = (connectTimeoutMs == 0) ? 3000 : connectTimeoutMs;
        readTimeoutMs = (readTimeoutMs == 0) ? 5000 : readTimeoutMs;
        retryMaxAttempts = (retryMaxAttempts == 0) ? 3 : retryMaxAttempts;
        retryBackoffMs = (retryBackoffMs == 0) ? 300 : retryBackoffMs;
        numOfRows = (numOfRows == 0) ? 1000 : numOfRows;
    }

    @AssertTrue(message = "authKey must be present when weather.kma.enabled=true")
    public boolean isAuthKeyValidWhenEnabled() {
        return !enabled || (authKey != null && !authKey.isBlank());
    }
}
