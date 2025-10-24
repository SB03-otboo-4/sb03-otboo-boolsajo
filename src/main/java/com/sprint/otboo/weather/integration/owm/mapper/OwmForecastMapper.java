package com.sprint.otboo.weather.integration.owm.mapper;

import static java.util.Optional.ofNullable;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class OwmForecastMapper {

    private OwmForecastMapper() {}

    public static List<CollectedForecast> toCollected(OwmForecastResponse res) {
        List<CollectedForecast> out = new ArrayList<>();
        if (res == null || res.list() == null) return out;

        for (OwmForecastResponse.Item i : res.list()) {
            Integer weatherId = (i.weather() != null && !i.weather().isEmpty())
                ? i.weather().get(0).id() : null;

            double temperature = nz(ofNullable(i.main()).map(OwmForecastResponse.Main::temp).orElse(null));
            int humidityPct   = nzInt(ofNullable(i.main()).map(OwmForecastResponse.Main::humidity).orElse(null));
            double windMs     = nz(ofNullable(i.wind()).map(OwmForecastResponse.Wind::speed).orElse(null));
            double rain3hMm   = nz(ofNullable(i.rain()).map(OwmForecastResponse.Rain::_3h).orElse(null));
            double snow3hMm   = nz(ofNullable(i.snow()).map(OwmForecastResponse.Snow::_3h).orElse(null));
            int cloudsAll     = nzInt(ofNullable(i.clouds()).map(OwmForecastResponse.Clouds::all).orElse(null));

            out.add(new CollectedForecast(
                Instant.ofEpochSecond(i.dt()),
                temperature,
                humidityPct,
                windMs,
                i.pop(),          // 0..1
                rain3hMm,
                snow3hMm,
                cloudsAll,        // 0~100
                weatherId
            ));
        }
        return out;
    }

    private static double nz(Double v) { return v == null ? 0d : v; }
    private static int nzInt(Integer v) { return v == null ? 0 : v; }
}
