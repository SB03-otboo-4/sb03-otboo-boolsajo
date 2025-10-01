package com.sprint.otboo.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.weather.integration.kma.WeatherKmaProperties;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClientImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WeatherKmaProperties.class)
public class KmaConfig {

    @Bean
    public KmaShortTermForecastClient kmaShortTermForecastClient(
        WeatherKmaProperties props,
        ObjectMapper objectMapper
    ) {
        return new KmaShortTermForecastClientImpl(props, objectMapper);
    }
}
