package com.sprint.otboo.weather.service;

import com.sprint.otboo.common.util.KmaGridConverter;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherLocationQueryServiceImpl implements WeatherLocationQueryService {

    private final WeatherLocationRepository repo;

    @Override
    public WeatherLocationResponse getWeatherLocation(double latitude, double longitude) {

        // 1) 위경도 저장값이 있으면 반환
        return repo.findFirstByLatitudeAndLongitude(latitude, longitude)
            .map(WeatherMapper::toLocationResponse)
            .orElseGet(() -> {
                // 2) 없으면 격자 계산 + 빈 이름 리스트
                KmaGridConverter.XY xy = KmaGridConverter.toXY(latitude, longitude);
                return new WeatherLocationResponse(latitude, longitude, xy.x(), xy.y(), List.of());
            });
    }
}