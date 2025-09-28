package com.sprint.otboo.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBatchTest
@SpringBootTest
@EnableBatchProcessing
@ActiveProfiles("test")
@DisplayName("배치: WeatherForecastJob 통합 테스트")
class WeatherForecastJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private WeatherLocationRepository locationRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @MockitoBean
    private KmaShortTermForecastClient kmaClient;

    @Autowired
    private Job weatherForecastJob;

    @Test
    void 활성_위치들을_수집하여_DB에_반영한다() throws Exception {
        // given
        WeatherLocation seoul = WeatherLocation.builder().build();
        seoul.setId(UUID.randomUUID());
        seoul.setX(60); seoul.setY(127);
        seoul.setLatitude(new java.math.BigDecimal("37.5665"));
        seoul.setLongitude(new java.math.BigDecimal("126.9780"));
        seoul.setLocationNames("서울특별시 중구");
        locationRepository.save(seoul);

        WeatherLocation busan = WeatherLocation.builder().build();
        busan.setId(UUID.randomUUID());
        busan.setX(98); busan.setY(76);
        busan.setLatitude(new java.math.BigDecimal("35.1796"));
        busan.setLongitude(new java.math.BigDecimal("129.0756"));
        busan.setLocationNames("부산광역시 중구");
        locationRepository.save(busan);

        KmaForecastResponse resp = dummyResponse("20250928", "0900", "1200");
        when(kmaClient.getVilageFcst(anyMap())).thenReturn(resp);

        long before = weatherRepository.count();

        // when
        JobParametersBuilder params = new JobParametersBuilder()
            .addLong("executionTime", System.currentTimeMillis());
        JobExecution execution = jobLauncher.run(weatherForecastJob, params.toJobParameters());

        // then
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
        long after = weatherRepository.count();

        assertThat(after).isGreaterThanOrEqualTo(before + 4);
    }

    @Test
    void 클라이언트_일시_실패시_재시도_후_완료된다() throws Exception {
        // given: 위치 1건
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setId(UUID.randomUUID());
        wl.setX(60); wl.setY(127);
        wl.setLatitude(new java.math.BigDecimal("37.5665"));
        wl.setLongitude(new java.math.BigDecimal("126.9780"));
        wl.setLocationNames("서울특별시 중구");
        locationRepository.save(wl);


        KmaForecastResponse ok = dummyResponse("20250928", "1500");
        when(kmaClient.getVilageFcst(anyMap()))
            .thenThrow(new RuntimeException("429/5xx 시뮬레이션"))
            .thenReturn(ok);

        long before = weatherRepository.count();

        // when
        JobParametersBuilder params = new JobParametersBuilder()
            .addLong("executionTime", System.currentTimeMillis());
        JobExecution execution = jobLauncher.run(weatherForecastJob, params.toJobParameters());

        // then
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
        assertThat(weatherRepository.count()).isGreaterThan(before);
    }

    private KmaForecastResponse dummyResponse(String date, String... times) {
        List<KmaForecastItem> items = new ArrayList<>();
        for (String t : times) {

            KmaForecastItem tmp = new KmaForecastItem();
            tmp.setCategory("TMP"); tmp.setFcstDate(date); tmp.setFcstTime(t); tmp.setFcstValue("24");
            items.add(tmp);

            KmaForecastItem reh = new KmaForecastItem();
            reh.setCategory("REH"); reh.setFcstDate(date); reh.setFcstTime(t); reh.setFcstValue("60");
            items.add(reh);

            KmaForecastItem pop = new KmaForecastItem();
            pop.setCategory("POP"); pop.setFcstDate(date); pop.setFcstTime(t); pop.setFcstValue("20");
            items.add(pop);

            KmaForecastItem pty = new KmaForecastItem();
            pty.setCategory("PTY"); pty.setFcstDate(date); pty.setFcstTime(t); pty.setFcstValue("0");
            items.add(pty);

            KmaForecastItem sky = new KmaForecastItem();
            sky.setCategory("SKY"); sky.setFcstDate(date); sky.setFcstTime(t); sky.setFcstValue("3");
            items.add(sky);
        }
        KmaForecastResponse resp = new KmaForecastResponse();
        resp.setResultCode("00");
        resp.setItems(items);
        return resp;
    }
}
