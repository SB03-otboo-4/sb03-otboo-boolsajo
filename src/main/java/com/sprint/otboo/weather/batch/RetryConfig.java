package com.sprint.otboo.weather.batch;

import com.sprint.otboo.weather.integration.kma.WeatherKmaProperties;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {
    @Bean
    public RetryTemplate weatherRetryTemplate() {
        RetryTemplate t = new RetryTemplate();

        SimpleRetryPolicy policy = new SimpleRetryPolicy(
            3, Map.of(RuntimeException.class, true), true
        );
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(100L);

        t.setRetryPolicy(policy);
        t.setBackOffPolicy(backOff);
        return t;
    }
}
