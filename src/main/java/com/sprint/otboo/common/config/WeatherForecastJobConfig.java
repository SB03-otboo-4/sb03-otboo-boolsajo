package com.sprint.otboo.common.config;

import com.sprint.otboo.weather.batch.task.WeatherCollectTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class WeatherForecastJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean(name = "weatherForecastJob")
    public Job weatherForecastJob(Step collectForecastStep) {
        return new JobBuilder("weatherForecastJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(collectForecastStep)
            .build();
    }

    @Bean
    public Step collectForecastStep(WeatherCollectTasklet tasklet) {
        return new StepBuilder("collectForecastStep", jobRepository)
            .tasklet(tasklet, transactionManager)
            .build();
    }
}
