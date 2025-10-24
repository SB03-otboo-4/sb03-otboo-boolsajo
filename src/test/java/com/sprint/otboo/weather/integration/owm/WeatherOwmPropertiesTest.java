package com.sprint.otboo.weather.integration.owm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("WeatherOwmProperties 보정/검증 테스트")
class WeatherOwmPropertiesTest {

    private final ApplicationContextRunner baseRunner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(WeatherOwmProperties.class)
    static class TestConfig { }

    @Test
    void 바인딩과_기본값_보정이_적용된다() {
        ApplicationContextRunner runner = baseRunner.withPropertyValues(
            "weather.owm.enabled=true",
            "weather.owm.api-key=abc", // NotBlank 통과

            // 의도적으로 지저분한 baseUrl: 말단 forecast/여분 슬래시 포함
            "weather.owm.base-url=https://api.openweathermap.org/data/2.5/forecast///",

            // 0이나 공백 → 내부 기본값/보정 기대
            "weather.owm.connect-timeout-ms=0",
            "weather.owm.read-timeout-ms=0",
            "weather.owm.retry-max-attempts=0",
            "weather.owm.retry-backoff-ms=0",
            "weather.owm.units=",
            "weather.owm.lang=",

            // 확률 퍼센트 사용 케이스
            "weather.owm.probability-percent=true"
        );

        runner.run(context -> {
            WeatherOwmProperties p = context.getBean(WeatherOwmProperties.class);

            // baseUrl 보정: forecast 꼬리 제거 및 정규화
            String baseUrl = p.baseUrl();
            assertThat(baseUrl).isNotBlank();
            assertThat(baseUrl).doesNotEndWith("/forecast");

            // 시간/재시도 기본값 보정(0 입력했어도 >0)
            assertThat(p.connectTimeoutMs()).isGreaterThan(0);
            assertThat(p.readTimeoutMs()).isGreaterThan(0);
            assertThat(p.retryMaxAttempts()).isGreaterThan(0);
            assertThat(p.retryBackoffMs()).isGreaterThan(0);

            // 공백 units/lang → 합리적 기본값으로 보정 (구현 의도: metric / ko-KR 등)
            assertThat(p.units()).isNotBlank();
            assertThat(p.lang()).isNotBlank();

            // 옵션 유지
            assertThat(p.probabilityPercent()).isTrue();
        });
    }

    @Test
    void enabled_true에서_apiKey가_비어있으면_컨텍스트_자체가_실패한다() {
        ApplicationContextRunner runner = baseRunner.withPropertyValues(
            "weather.owm.enabled=true",
            "weather.owm.api-key=   " // 공백 → @NotBlank 위반
        );

        runner.run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isInstanceOf(ConfigurationPropertiesBindException.class);
            assertThat(failure.getMessage()).contains("weather.owm", "apiKey");
        });
    }
}
