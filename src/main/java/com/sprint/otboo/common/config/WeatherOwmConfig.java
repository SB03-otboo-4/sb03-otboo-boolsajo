package com.sprint.otboo.common.config;

import com.sprint.otboo.weather.integration.owm.WeatherOwmProperties;
import com.sprint.otboo.weather.integration.owm.client.OwmClientAdapter;
import com.sprint.otboo.weather.integration.owm.client.OwmForecastClient;
import com.sprint.otboo.weather.integration.owm.client.OwmForecastClientImpl;
import com.sprint.otboo.weather.integration.owm.mapper.DefaultWindStrengthResolver;
import com.sprint.otboo.weather.integration.owm.mapper.WindStrengthResolver;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(WeatherOwmProperties.class)
@ConditionalOnProperty(prefix = "weather.owm", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WeatherOwmConfig {

    private final WeatherOwmProperties props;

    @Bean
    @Qualifier("owmWebClient")
    public WebClient owmWebClient(Builder builder) {
        return builder
            .baseUrl(props.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
                    .responseTimeout(Duration.ofMillis(props.readTimeoutMs()))
            ))
            .build();
    }

    @Bean
    public OwmForecastClient owmForecastClient(@Qualifier("owmWebClient") WebClient client) {
        return new OwmForecastClientImpl(
            client, props.apiKey(), props.baseUrl(), props.units(), props.lang()
        );
    }

    @Bean
    public WeatherDataClient weatherDataClient(OwmForecastClient client) {
        return new OwmClientAdapter(client);
    }

    @Bean
    public WindStrengthResolver windStrengthResolver() {
        return new DefaultWindStrengthResolver();
    }

    // ⚠️ Assembler는 요청마다 daily-map/zone이 달라지므로 싱글턴 빈으로 두지 않음

    @Bean
    public Locale owmDefaultLocale() {
        return Locale.forLanguageTag(props.lang());
    }
}
