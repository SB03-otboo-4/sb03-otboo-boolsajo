package com.sprint.otboo.weather.integration.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.api")
public record KakaoApiProperties(
    String baseUrl,
    String restApiKey
) {}