package com.sprint.otboo.common.config;

import com.sprint.otboo.weather.integration.owm.WeatherOwmProperties;
import com.sprint.otboo.weather.integration.owm.client.OwmClientAdapter;
import com.sprint.otboo.weather.integration.owm.client.OwmForecastClient;
import com.sprint.otboo.weather.integration.owm.client.OwmForecastClientImpl;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.mapper.DefaultWindStrengthResolver;
import com.sprint.otboo.weather.mapper.OwmForecastAssembler;
import com.sprint.otboo.weather.mapper.WindStrengthResolver;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(WeatherOwmProperties.class)
@ConditionalOnProperty(prefix = "weather.owm", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class WeatherOwmConfig {

    private final WeatherOwmProperties props;

    @Bean
    public WebClient owmWebClient(WebClient.Builder builder) {
        // (선택) 타임아웃 반영 — Reactor Netty 기반
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
            .responseTimeout(Duration.ofMillis(props.readTimeoutMs()))
            .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(props.readTimeoutMs())));

        return builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public OwmForecastClient owmForecastClient(WebClient owmWebClient) {
        return new OwmForecastClientImpl(
            owmWebClient,
            props.apiKey(),
            props.baseUrl()
        );
    }

    @Bean
    public WeatherDataClient owmDataClient(OwmForecastClient client) {
        return new OwmClientAdapter(client);
    }

    @Bean
    public WindStrengthResolver windStrengthResolver() {
        return new DefaultWindStrengthResolver();
    }

    @Bean
    public OwmForecastAssembler owmForecastAssembler(WindStrengthResolver resolver) {
        return new OwmForecastAssembler(props.probabilityPercent(), resolver);
    }

    @Bean
    public Locale owmDefaultLocale() {
        return Locale.forLanguageTag(props.lang());
    }
}
