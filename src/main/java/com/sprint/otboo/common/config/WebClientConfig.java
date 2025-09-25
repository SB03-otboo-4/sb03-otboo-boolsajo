package com.sprint.otboo.common.config;

import com.sprint.otboo.weather.integration.kakao.KakaoApiProperties;
import com.sprint.otboo.weather.integration.kma.WeatherKmaProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({KakaoApiProperties.class, WeatherKmaProperties.class})
public class WebClientConfig {

    @Bean(name = "kakaoWebClient")
    public WebClient kakaoWebClient(KakaoApiProperties props) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)    // 연결 타임아웃 2s
            .responseTimeout(Duration.ofSeconds(3))                // 응답 타임아웃 3s
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + props.restApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
