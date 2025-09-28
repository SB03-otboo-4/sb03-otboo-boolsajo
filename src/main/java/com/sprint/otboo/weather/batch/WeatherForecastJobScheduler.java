package com.sprint.otboo.weather.batch;

import java.util.Date;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeatherForecastJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job weatherForecastJob;

    public WeatherForecastJobScheduler(JobLauncher jobLauncher, Job weatherForecastJob) {
        this.jobLauncher = jobLauncher;
        this.weatherForecastJob = weatherForecastJob;
    }

    @Scheduled(cron = "${weather.batch.cron:0 30 * * * *}")
    public void run() throws Exception {
        jobLauncher.run(
            weatherForecastJob,
            new JobParametersBuilder()
                .addDate("executionTime", new Date())
                .toJobParameters()
        );
    }
}
