package com.sprint.otboo.weather.service;

import com.sprint.otboo.common.exception.weather.WeatherBadCoordinateException;
import com.sprint.otboo.common.util.GeoNormalize;
import com.sprint.otboo.common.util.KmaGridConverter;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeatherLocationQueryServiceImpl implements WeatherLocationQueryService {

    private final WeatherLocationRepository repository;
    private final LocationNameResolver locationNameResolver;

    @Override
    @Transactional // 저장 로직이 있으므로 readOnly=false (기본값)로 명시
    public WeatherLocationResponse getWeatherLocation(double latitude, double longitude) {
        validate(latitude, longitude);

        // 1) 위/경도 정규화 (소수 6자리 등 프로젝트 정책에 맞춰 GeoNormalize 사용)
        BigDecimal normalizedLat = GeoNormalize.lat(latitude);
        BigDecimal normalizedLon = GeoNormalize.lon(longitude);

        // 2) (lat, lon) 기준 선조회 → 있으면 즉시 반환
        Optional<WeatherLocation> byLatLon = repository.findFirstByLatitudeAndLongitude(normalizedLat, normalizedLon);
        if (byLatLon.isPresent()) {
            WeatherLocation found = byLatLon.get();
            return WeatherMapper.toLocationResponse(found);
        }

        // 3) 격자 변환 (KMA 좌표계)
        KmaGridConverter.XY xy = KmaGridConverter.toXY(latitude, longitude);

        // 4) (x, y) 기준 선조회 → 있으면 재사용
        Optional<WeatherLocation> byXY = repository.findFirstByXAndY(xy.x(), xy.y());
        if (byXY.isPresent()) {
            WeatherLocation reused = byXY.get();
            return WeatherMapper.toLocationResponse(reused);
        }

        // 5) 카카오 지역명 조회 (장애/오류 시 빈 리스트 폴백)
        List<String> locationNames;
        try {
            locationNames = locationNameResolver.resolve(latitude, longitude);
        } catch (Exception ex) {
            locationNames = List.of();
        }

        // 6) 신규 엔티티 생성 및 저장
        WeatherLocation weatherLocation = new WeatherLocation();
        weatherLocation.setLatitude(normalizedLat);
        weatherLocation.setLongitude(normalizedLon);
        weatherLocation.setX(xy.x());
        weatherLocation.setY(xy.y());
        weatherLocation.setLocationNames(locationNames.isEmpty() ? null : String.join(" ", locationNames));
        weatherLocation.setCreatedAt(Instant.now());

        WeatherLocation saved = repository.save(weatherLocation);

        // 7) DTO 매핑 후 반환
        return WeatherMapper.toLocationResponse(saved);
    }

    private void validate(double latitude, double longitude) {
        boolean invalidLat = latitude < -90.0d || latitude > 90.0d;
        boolean invalidLon = longitude < -180.0d || longitude > 180.0d;
        if (invalidLat || invalidLon) {
            throw new WeatherBadCoordinateException();
        }
    }
}