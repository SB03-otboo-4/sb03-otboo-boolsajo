package com.sprint.otboo.weather.change;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WeatherChangeThresholdProperties.class)
public class ChangeConfig {

}
