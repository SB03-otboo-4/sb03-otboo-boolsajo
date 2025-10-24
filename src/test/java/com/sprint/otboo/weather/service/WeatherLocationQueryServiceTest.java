package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("WeatherLocationQueryService 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherLocationQueryServiceTest {

    @Mock
    private WeatherLocationRepository repo;

    @Mock
    private LocationNameResolver resolver;

    @Mock
    private WeatherMapper mapper;

    private WeatherLocationQueryService service;

    @BeforeEach
    void setUp() {
        service = new WeatherLocationQueryServiceImpl(repo, resolver, mapper);
    }

    @Test
    void 저장된_위치가_있으면_DB_저장값으로_응답해야_한다() {
        // given: (lat,lon) 선조회가 Optional.of(...)를 반환
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setLatitude(new BigDecimal("37.5665"));
        wl.setLongitude(new BigDecimal("126.9780"));
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");
        wl.setCreatedAt(Instant.now());

        given(repo.findFirstByLatitudeAndLongitude(any(BigDecimal.class), any(BigDecimal.class)))
            .willReturn(Optional.of(wl));

        given(mapper.toLocationResponse(any(WeatherLocation.class))).willAnswer(inv -> {
            WeatherLocation e = inv.getArgument(0);
            List<String> names = (e.getLocationNames() == null || e.getLocationNames().isBlank())
                ? List.of()
                : List.of(e.getLocationNames().split("\\s+"));
            return new WeatherLocationResponse(
                e.getLatitude().doubleValue(),
                e.getLongitude().doubleValue(),
                e.getX(), e.getY(), names
            );
        });

        // when
        WeatherLocationResponse dto = service.getWeatherLocation(37.5665, 126.9780);

        // then
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).containsExactly("서울특별시", "중구", "태평로1가");
    }

    @Test
    void 저장된_위치가_없으면_격자_선조회후_미존재시_Resolver_결과로_신규저장한_응답이어야_한다() {
        // given: (lat,lon) 조회 = empty
        given(repo.findFirstByLatitudeAndLongitude(any(BigDecimal.class), any(BigDecimal.class)))
            .willReturn(Optional.empty());

        // given: (x,y) 조회도 empty → 신규 저장 경로
        given(repo.findFirstByXAndY(60, 127)).willReturn(Optional.empty());

        // given: resolver 정상 동작
        given(resolver.resolve(37.5665, 126.9780))
            .willReturn(List.of("서울특별시", "중구", "태평로1가"));

        // 저장되는 엔티티를 그대로 돌려주도록 스텁(간단화)
        given(repo.save(any(WeatherLocation.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(mapper.toLocationResponse(any(WeatherLocation.class))).willAnswer(inv -> {
            WeatherLocation e = inv.getArgument(0);
            List<String> names = (e.getLocationNames() == null || e.getLocationNames().isBlank())
                ? List.of()
                : List.of(e.getLocationNames().split("\\s+"));
            return new WeatherLocationResponse(
                e.getLatitude().doubleValue(),
                e.getLongitude().doubleValue(),
                e.getX(), e.getY(), names
            );
        });

        // when
        WeatherLocationResponse dto = service.getWeatherLocation(37.5665, 126.9780);

        // then: KMA 격자(서울 시청 인근) 60/127, 지역명 파싱 확인
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).containsExactly("서울특별시", "중구", "태평로1가");
    }
}
