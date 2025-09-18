package com.sprint.otboo.weather.service;

import com.sprint.otboo.common.exception.weather.WeatherBadCoordinateException;
import com.sprint.otboo.common.util.KmaGridConverter;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherLocationQueryServiceImpl implements WeatherLocationQueryService {

    private final WeatherLocationRepository repo;
    private final LocationNameResolver resolver;

    @Override
    public WeatherLocationResponse getWeatherLocation(double latitude, double longitude) {
        validate(latitude, longitude);

        return repo.findFirstByLatitudeAndLongitude(latitude, longitude)
            .map(WeatherMapper::toLocationResponse)
            .orElseGet(() -> {
                KmaGridConverter.XY xy = KmaGridConverter.toXY(latitude, longitude);
                List<String> names = resolver.resolve(latitude, longitude);
                return new WeatherLocationResponse(latitude, longitude, xy.x(), xy.y(), names);
            });
    }

    private void validate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new WeatherBadCoordinateException();
        }
    }
}