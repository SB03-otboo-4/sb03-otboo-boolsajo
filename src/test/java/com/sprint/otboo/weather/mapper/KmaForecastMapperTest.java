package com.sprint.otboo.weather.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KmaForecastMapper 테스트")
class KmaForecastMapperTest {

    private final KmaForecastMapper mapper = new DefaultKmaForecastMapper();

    @Test
    @DisplayName("SKY 코드값을 SkyStatus로 변환해야 한다 (1:맑음, 3:구름많음, 4:흐림)")
    void SKY_코드를_SkyStatus로_변환해야_한다() {
        assertThat(mapper.mapSky("1")).isEqualTo(SkyStatus.CLEAR);
        assertThat(mapper.mapSky("3")).isEqualTo(SkyStatus.MOSTLY_CLOUDY);
        assertThat(mapper.mapSky("4")).isEqualTo(SkyStatus.CLOUDY);
        // 알 수 없는 코드는 기본값(선호값: MOSTLY_CLOUDY)으로 처리
        assertThat(mapper.mapSky("999")).isEqualTo(SkyStatus.MOSTLY_CLOUDY);
        assertThat(mapper.mapSky(null)).isEqualTo(SkyStatus.MOSTLY_CLOUDY);
    }

    @Test
    @DisplayName("PTY 코드값을 PrecipitationType으로 변환해야 한다 (0:없음, 1:비, 2:비/눈, 3:눈)")
    void PTY_코드를_PrecipitationType으로_변환해야_한다() {
        assertThat(mapper.mapPrecipitation("0")).isEqualTo(PrecipitationType.NONE);
        assertThat(mapper.mapPrecipitation("1")).isEqualTo(PrecipitationType.RAIN);
        assertThat(mapper.mapPrecipitation("2")).isEqualTo(PrecipitationType.RAIN_SNOW);
        assertThat(mapper.mapPrecipitation("3")).isEqualTo(PrecipitationType.SNOW);
        assertThat(mapper.mapPrecipitation("4")).isEqualTo(PrecipitationType.SHOWER);
        assertThat(mapper.mapPrecipitation("5")).isEqualTo(PrecipitationType.RAIN);
        assertThat(mapper.mapPrecipitation("6")).isEqualTo(PrecipitationType.RAIN_SNOW);
        assertThat(mapper.mapPrecipitation("7")).isEqualTo(PrecipitationType.SNOW);
        assertThat(mapper.mapPrecipitation(null)).isEqualTo(PrecipitationType.NONE);
    }

    @Test
    @DisplayName("TMP, REH, POP 값을 숫자로 파싱하고 누락 시 기본값을 반환해야 한다")
    void TMP_REH_POP_숫자_파싱과_기본값_처리() {
        List<KmaForecastItem> items = new ArrayList<>();

        // TMP(기온)
        KmaForecastItem tmp = new KmaForecastItem();
        tmp.setCategory("TMP");
        tmp.setFcstDate("20250924");
        tmp.setFcstTime("1100");
        tmp.setFcstValue("24");
        items.add(tmp);

        // REH(습도)
        KmaForecastItem reh = new KmaForecastItem();
        reh.setCategory("REH");
        reh.setFcstDate("20250924");
        reh.setFcstTime("1100");
        reh.setFcstValue("60");
        items.add(reh);

        // POP(강수확률)
        KmaForecastItem pop = new KmaForecastItem();
        pop.setCategory("POP");
        pop.setFcstDate("20250924");
        pop.setFcstTime("1100");
        pop.setFcstValue("30");
        items.add(pop);

        int temperature = mapper.extractInt(items, "TMP", 999);
        int humidity = mapper.extractInt(items, "REH", 999);
        int precipitationProb = mapper.extractInt(items, "POP", 0);

        assertThat(temperature).isEqualTo(24);
        assertThat(humidity).isEqualTo(60);
        assertThat(precipitationProb).isEqualTo(30);

        // 누락 시 기본값
        int windSpeedMissing = mapper.extractInt(items, "WSD", -1);
        assertThat(windSpeedMissing).isEqualTo(-1);
    }

    @Test
    @DisplayName("SKY/PTY/TMP/REH/POP를 종합하여 하나의 슬롯으로 매핑해야 한다")
    void 카테고리_묶음을_하나의_슬롯으로_매핑해야_한다() {
        List<KmaForecastItem> items = new ArrayList<>();

        KmaForecastItem sky = new KmaForecastItem();
        sky.setCategory("SKY");
        sky.setFcstDate("20250924");
        sky.setFcstTime("1100");
        sky.setFcstValue("3");
        items.add(sky);

        KmaForecastItem pty = new KmaForecastItem();
        pty.setCategory("PTY");
        pty.setFcstDate("20250924");
        pty.setFcstTime("1100");
        pty.setFcstValue("1");
        items.add(pty);

        KmaForecastItem tmp = new KmaForecastItem();
        tmp.setCategory("TMP");
        tmp.setFcstDate("20250924");
        tmp.setFcstTime("1100");
        tmp.setFcstValue("24");
        items.add(tmp);

        KmaForecastItem reh = new KmaForecastItem();
        reh.setCategory("REH");
        reh.setFcstDate("20250924");
        reh.setFcstTime("1100");
        reh.setFcstValue("60");
        items.add(reh);

        KmaForecastItem pop = new KmaForecastItem();
        pop.setCategory("POP");
        pop.setFcstDate("20250924");
        pop.setFcstTime("1100");
        pop.setFcstValue("30");
        items.add(pop);

        // mapper가 반환하는 간단 슬롯 DTO(또는 값 객체)를 가정
        // 실제 구현에서는 프로젝트 내 WeatherSummaryDto 등으로 변환해도 됨
        KmaForecastMapper.Slot slot = mapper.toSlot(items, "20250924", "1100");

        assertThat(slot.getFcstDate()).isEqualTo("20250924");
        assertThat(slot.getFcstTime()).isEqualTo("1100");
        assertThat(slot.getSky()).isEqualTo(SkyStatus.MOSTLY_CLOUDY);
        assertThat(slot.getPrecipitation()).isEqualTo(PrecipitationType.RAIN);
        assertThat(slot.getTemperature()).isEqualTo(24);
        assertThat(slot.getHumidity()).isEqualTo(60);
        assertThat(slot.getPrecipitationProbability()).isEqualTo(30);
    }
}
