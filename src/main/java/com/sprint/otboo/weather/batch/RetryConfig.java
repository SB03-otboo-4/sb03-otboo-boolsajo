package com.sprint.otboo.weather.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate weatherRetryTemplate() {
        RetryTemplate tpl = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy(3);
        FixedBackOffPolicy backoff = new FixedBackOffPolicy();
        backoff.setBackOffPeriod(100L); // 100ms
        tpl.setRetryPolicy(policy);
        tpl.setBackOffPolicy(backoff);
        return tpl;
    }
}
