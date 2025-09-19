package com.sprint.otboo.weather.service;

import com.sprint.otboo.common.util.GeoNormalize;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.math.BigDecimal;
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
    @Transactional // ← 저장 위해 읽기전용 해제
    public WeatherLocationResponse getWeatherLocation(double latitude, double longitude) {
        validate(latitude, longitude);

        BigDecimal nLat = GeoNormalize.lat(latitude);
        BigDecimal nLon = GeoNormalize.lon(longitude);

        return repo.findFirstByLatitudeAndLongitude(nLat, nLon)
            .map(com.sprint.otboo.weather.mapper.WeatherMapper::toLocationResponse)
            .orElseGet(() -> {
                com.sprint.otboo.common.util.KmaGridConverter.XY xy =
                    com.sprint.otboo.common.util.KmaGridConverter.toXY(latitude, longitude);

                java.util.List<String> names;
                try {
                    names = resolver.resolve(latitude, longitude); // 성공 시 ["서울특별시","중구","..."]
                } catch (Exception e) {
                    names = java.util.List.of(); // 실패 시 빈 리스트 → 엔티티에서 GRID 대체
                }

                com.sprint.otboo.weather.entity.WeatherLocation wl =
                    new com.sprint.otboo.weather.entity.WeatherLocation();
                wl.setId(java.util.UUID.randomUUID());
                wl.setLatitude(nLat);
                wl.setLongitude(nLon);
                wl.setX(Integer.valueOf(xy.x()));
                wl.setY(Integer.valueOf(xy.y()));
                wl.setLocationNames(names.isEmpty() ? null : String.join(" ", names)); // PrePersist에서 최종 보정
                wl.setCreatedAt(java.time.Instant.now());

                repo.save(wl);
                return com.sprint.otboo.weather.mapper.WeatherMapper.toLocationResponse(wl);
            });
    }

    private void validate(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new com.sprint.otboo.common.exception.weather.WeatherBadCoordinateException();
        }
    }
}