package com.sprint.otboo.weather.integration.owm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OWM 추론 규칙 테스트")
class OwmInferenceTest {

    @Test
    @DisplayName("clouds.all 기반 skyStatus 추론 (경계값 포함)")
    void clouds_all_기반_skyStatus_추론() {
        // clouds 우선 규칙
        assertThat(OwmInference.toSkyStatus(0, null).name()).isEqualTo("CLEAR");
        assertThat(OwmInference.toSkyStatus(10, null).name()).isEqualTo("CLEAR");
        assertThat(OwmInference.toSkyStatus(11, null).name()).isEqualTo("MOSTLY_CLOUDY");
        assertThat(OwmInference.toSkyStatus(70, null).name()).isEqualTo("MOSTLY_CLOUDY");
        assertThat(OwmInference.toSkyStatus(71, null).name()).isEqualTo("CLOUDY");
        assertThat(OwmInference.toSkyStatus(100, null).name()).isEqualTo("CLOUDY");
    }

    @Test
    void weather_id가_명시될_경우_코드_우선_보정() {
        assertThat(OwmInference.toSkyStatus(90, 800).name()).isEqualTo("CLEAR");          // clear
        assertThat(OwmInference.toSkyStatus(5, 803).name()).isEqualTo("CLOUDY");          // broken
        assertThat(OwmInference.toSkyStatus(5, 804).name()).isEqualTo("CLOUDY");          // overcast
        assertThat(OwmInference.toSkyStatus(5, 801).name()).isEqualTo("MOSTLY_CLOUDY");   // few
        assertThat(OwmInference.toSkyStatus(5, 802).name()).isEqualTo("MOSTLY_CLOUDY");   // scattered
    }

    @Test
    void 강수_타입_매핑() {
        // 2xx
        assertThat(OwmInference.toPrecipType(200).name()).isEqualTo("SHOWER");
        // 3xx
        assertThat(OwmInference.toPrecipType(300).name()).isEqualTo("RAIN");
        // 5xx
        assertThat(OwmInference.toPrecipType(520).name()).isEqualTo("SHOWER");
        assertThat(OwmInference.toPrecipType(511).name()).isEqualTo("RAIN_SNOW");
        assertThat(OwmInference.toPrecipType(500).name()).isEqualTo("RAIN");
        // 6xx
        assertThat(OwmInference.toPrecipType(611).name()).isEqualTo("RAIN_SNOW"); // sleet
        assertThat(OwmInference.toPrecipType(600).name()).isEqualTo("SNOW");
        // 7xx & 기타
        assertThat(OwmInference.toPrecipType(701).name()).isEqualTo("NONE");
        assertThat(OwmInference.toPrecipType(null).name()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("강수확률 POP 정규화(항상 0..1)")
    void 강수_확률_POP_정규화() {
        // 입력이 %인 경우 (inputIsPercent = true) → 0..1로 변환
        assertThat(OwmInference.toProbabilityNormalized(null, true)).isEqualTo(0d);
        assertThat(OwmInference.toProbabilityNormalized(0d, true)).isEqualTo(0d);
        assertThat(OwmInference.toProbabilityNormalized(32d, true)).isEqualTo(0.32d);
        assertThat(OwmInference.toProbabilityNormalized(100d, true)).isEqualTo(1.0d);

        // 입력이 소수(0..1)인 경우 (inputIsPercent = false) → 그대로 0..1 유지
        assertThat(OwmInference.toProbabilityNormalized(0.32, false)).isEqualTo(0.32d);

        // 방어로직: 실수로 1보다 큰 소수가 들어와도 0..1로 보정
        assertThat(OwmInference.toProbabilityNormalized(2.5, false)).isEqualTo(0.025d);
        assertThat(OwmInference.toProbabilityNormalized(250d, false)).isEqualTo(1.0d); // 최종 clamp
    }
}
