package com.sprint.otboo.weather.service;

import com.sprint.otboo.common.util.KmaGridConverter;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherLocationQueryServiceImpl implements WeatherLocationQueryService {

    private final WeatherLocationRepository repo;

    @Override
    public WeatherLocationResponse getWeatherLocation(double longitude, double latitude) {

        // 1) 위경도 저장값이 있으면 반환
        Optional<WeatherLocation> found = repo.findFirstByLatitudeAndLongitude(latitude, longitude);
        if (found.isPresent()) {
            return WeatherMapper.toLocationResponse(found.get());
        }
        // 2) 없으면 격자 계산 후, 이름은 빈 리스트로 응답(RED에서 정의한 기대)
        KmaGridConverter.XY xy = KmaGridConverter.toXY(latitude, longitude);
        return new WeatherLocationResponse(latitude, longitude, xy.x(), xy.y(), List.of());
    }
}