package com.sprint.otboo.weather.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class WeatherLocationRepositoryTest {

    @Autowired
    private WeatherLocationRepository repo;

    @Test
    void xy로_저장된_위치를_조회할_수_있어야_한다() {
        WeatherLocation wl = new WeatherLocation();
        wl.setId(UUID.randomUUID());
        wl.setLatitude(37.5665);
        wl.setLongitude(126.9780);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");
        repo.save(wl);

        assertThat(repo.findByXAndY(60, 127)).isPresent();
    }

    @Test
    void 존재하지_않는_xy는_조회되지_않아야_한다() {
        assertThat(repo.findByXAndY(999, 999)).isNotPresent();
    }
}
