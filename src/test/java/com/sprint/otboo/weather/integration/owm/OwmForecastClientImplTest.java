package com.sprint.otboo.weather.integration.owm;

import com.sprint.otboo.weather.integration.owm.client.OwmForecastClientImpl;
import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@DisplayName("OwmForecastClientImpl 정상/에러/파싱 및 언어코드 분기 테스트")
class OwmForecastClientImplTest {

    @Test
    void 언어코드_정상화_로직이_분기별로_동작한다() throws Exception {
        OwmForecastClientImpl client = newClient(alwaysOkObjectBody());
        Method normalize = OwmForecastClientImpl.class.getDeclaredMethod("normalizeLang", Locale.class);
        normalize.setAccessible(true);

        String d1 = (String) normalize.invoke(client, (Object) null);
        String d2 = (String) normalize.invoke(client, new Locale("ko", "KR"));
        String d3 = (String) normalize.invoke(client, new Locale("ko"));
        String d4 = (String) normalize.invoke(client, Locale.forLanguageTag("EN-us"));
        String d5 = (String) normalize.invoke(client, Locale.forLanguageTag("pt-BR"));

        Assertions.assertThat(d1).isNotBlank();
        Assertions.assertThat(d2).isEqualTo("kr");
        Assertions.assertThat(d3).isEqualTo("kr");
        Assertions.assertThat(d4).isEqualTo("en");
        Assertions.assertThat(d5).isEqualTo("pt");
    }

    @Test
    void 오류_상태코드는_예외를_발생시킨다() {
        Function<ClientRequest, Mono<ClientResponse>> fx = request -> {
            ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
            return Mono.just(response);
        };

        OwmForecastClientImpl client = newClient(fx);

        Assertions.assertThatThrownBy(() -> client.get5Day3Hour(0.0, 0.0, Locale.KOREA))
            .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void 본문_JSON_파싱_실패시_예외가_발생한다() {
        Function<ClientRequest, Mono<ClientResponse>> fx = request -> {
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(Arrays.toString("not-json".getBytes(StandardCharsets.UTF_8)))
                .build();
            return Mono.just(response);
        };

        OwmForecastClientImpl client = newClient(fx);

        Assertions.assertThatThrownBy(() -> client.get5Day3Hour(37.5, 127.0, Locale.KOREA))
            .isInstanceOf(Exception.class);
    }

    // ===== helper methods =====

    private static OwmForecastClientImpl newClient(Function<ClientRequest, Mono<ClientResponse>> fx) {
        WebClient webClient = WebClient.builder().exchangeFunction(fx::apply).build();
        // 생성자 시그니처: (WebClient webClient, String apiKey, String baseUrl, String units, String defaultLang)
        return new OwmForecastClientImpl(webClient, "API_KEY", "http://unused-base", "metric", "kr");
    }

    private static Function<ClientRequest, Mono<ClientResponse>> alwaysOkObjectBody() {
        return request -> {
            String body = """
                {
                  "list": [],
                  "city": { "timezone": 32400 }
                }
                """;
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(Arrays.toString(body.getBytes(StandardCharsets.UTF_8)))
                .build();
            return Mono.just(response);
        };
    }
}
