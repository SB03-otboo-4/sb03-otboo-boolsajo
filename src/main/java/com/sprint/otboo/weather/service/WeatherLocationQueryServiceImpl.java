package com.sprint.otboo.weather.service;

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

        // 1) 위경도 저장값이 있으면 반환
        return repo.findFirstByLatitudeAndLongitude(latitude, longitude)
            .map(WeatherMapper::toLocationResponse)
            .orElseGet(() -> {
                // 2) 없으면 격자 계산 + Resolver로 행정구역 이름 조회
                KmaGridConverter.XY xy = KmaGridConverter.toXY(latitude, longitude);
                List<String> names = resolver.resolve(latitude, longitude);
                return new WeatherLocationResponse(latitude, longitude, xy.x(), xy.y(), names);
            });
    }
}