package com.sprint.otboo.weather.integration.owm.client;

import com.sprint.otboo.weather.integration.owm.mapper.OwmForecastMapper;
import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OwmClientAdapter implements WeatherDataClient {

    private final OwmForecastClient client;

    @Override
    public List<CollectedForecast> fetch(double latitude, double longitude, Locale locale) {
        OwmForecastResponse res = client.get5Day3Hour(latitude, longitude, locale);
        return OwmForecastMapper.toCollected(res);
    }
}
