package com.sprint.otboo.weather.batch;

import com.sprint.otboo.weather.batch.task.WeatherCollectTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherForecastJobConfig {

    @Bean
    public Job weatherForecastJob(
        JobBuilderFactory jobs,
        Step weatherCollectStep
    ) {
        return jobs.get("weatherForecastJob")
            .start(weatherCollectStep)
            .build();
    }

    @Bean
    public Step weatherCollectStep(
        StepBuilderFactory steps,
        WeatherCollectTasklet tasklet
    ) {
        return steps.get("weatherCollectStep")
            .tasklet(tasklet)
            .build();
    }
}
