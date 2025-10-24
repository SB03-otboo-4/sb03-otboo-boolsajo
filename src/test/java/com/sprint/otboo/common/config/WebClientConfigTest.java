package com.sprint.otboo.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.integration.kakao.KakaoApiProperties;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("WebClientConfig 테스트")
class WebClientConfigTest {

    static MockWebServer server;

    @BeforeAll
    static void init() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void shutdown() throws Exception {
        server.shutdown();
    }

    @Test
    void kakaoWebClient가_프로퍼티로_설정된_baseUrl과_Auth헤더를_사용한다() {
        String base = server.url("/").toString();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getSystemProperties().put("kakao.api.base-url", base);
        ctx.getEnvironment().getSystemProperties().put("kakao.api.rest-api-key", "dummy-key-123");

        ctx.register(TestConfig.class, WebClientConfig.class);
        ctx.refresh();

        WebClient wc = (WebClient) ctx.getBean("kakaoWebClient");
        WebClient.RequestHeadersSpec<?> spec = wc.get().uri("/ping");

        assertThat(wc).isNotNull();
        assertThat(spec).isNotNull();

        ctx.close();
    }

    @EnableConfigurationProperties(KakaoApiProperties.class)
    static class TestConfig {}
}