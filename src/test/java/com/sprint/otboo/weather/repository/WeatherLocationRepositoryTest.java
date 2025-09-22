package com.sprint.otboo.weather.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.weather.entity.WeatherLocation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WeatherLocationRepository 테스트")
class WeatherLocationRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {
        @PersistenceContext
        private EntityManager em;

        @Bean
        JPAQueryFactory jpaQueryFactory() {
            return new JPAQueryFactory(em);
        }
    }

    @Autowired
    private WeatherLocationRepository repo;

    @Test
    void xy로_저장된_위치를_조회할_수_있어야_한다() {
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setId(UUID.randomUUID());
        wl.setLatitude(new BigDecimal("37.5665"));
        wl.setLongitude(new BigDecimal("126.9780"));
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");
        repo.save(wl);

        assertThat(repo.findFirstByXAndY(60, 127)).isPresent();
    }

    @Test
    void 존재하지_않는_xy는_조회되지_않아야_한다() {
        assertThat(repo.findFirstByXAndY(999, 999)).isNotPresent();
    }
}
