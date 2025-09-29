package com.sprint.otboo.weather.change;

import java.util.Map;

public record DetectedChange(WeatherChangeType type, Map<String, Object> detail) {

}
