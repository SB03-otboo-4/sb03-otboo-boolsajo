package com.sprint.otboo.weather.integration.owm;

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

            out.add(new CollectedForecast(
                Instant.ofEpochSecond(i.dt()),
                nz(i.main() == null ? null : i.main().temp()),
                nzInt(i.main() == null ? null : i.main().humidity()),
                ofNullable(i.wind()).map(OwmForecastResponse.Wind::speed).orElse(null),
                i.pop(),
                ofNullable(i.rain()).map(OwmForecastResponse.Rain::_3h).orElse(null),
                ofNullable(i.snow()).map(OwmForecastResponse.Snow::_3h).orElse(null),
                ofNullable(i.clouds()).map(OwmForecastResponse.Clouds::all).orElse(null),
                weatherId
            ));
        }
        return out;
    }

    private static double nz(Double v) { return v == null ? 0d : v; }
    private static Integer nzInt(Integer v) { return v == null ? 0 : v; }
}
