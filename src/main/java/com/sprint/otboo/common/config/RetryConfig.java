package com.sprint.otboo.common.config;

import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import software.amazon.awssdk.core.exception.SdkException;

@Configuration
public class RetryConfig {

    @Bean("profileImageStorageRetryTemplate")
    public RetryTemplate profileImageStorageRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(500L); // 재시도 간격 0.5초
        template.setBackOffPolicy(backOff);

        SimpleRetryPolicy policy = new SimpleRetryPolicy(
            3, // 최대 3회까지 시도
            Map.of(
                IOException.class, true,
                SdkException.class, true,
                RuntimeException.class, true
            ),
            true
        );
        template.setRetryPolicy(policy);

        return template;
    }
}