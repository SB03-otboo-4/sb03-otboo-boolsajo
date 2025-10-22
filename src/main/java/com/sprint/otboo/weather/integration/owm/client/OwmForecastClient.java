package com.sprint.otboo.weather.integration.owm.client;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import java.util.Locale;

public interface OwmForecastClient {
    OwmForecastResponse get5Day3Hour(double lat, double lon, Locale locale);
}
