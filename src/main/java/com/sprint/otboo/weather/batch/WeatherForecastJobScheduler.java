package com.sprint.otboo.weather.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class WeatherForecastJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job weatherForecastJob;

    @Scheduled(cron = "${weather.batch.cron:0 30 * * * *}")
    public void run() {
        try {
            jobLauncher.run(
                weatherForecastJob,
                new JobParametersBuilder()
                    .addLong("executionTime", System.currentTimeMillis())
                    .toJobParameters()
            );
        } catch (Exception e) {
            log.error("weatherForecastJob failed", e);
        }
    }
}
