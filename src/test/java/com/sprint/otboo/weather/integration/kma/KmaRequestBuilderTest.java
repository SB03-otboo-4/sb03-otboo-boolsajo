package com.sprint.otboo.weather.integration.kma;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KmaRequestBuilder 테스트")
class KmaRequestBuilderTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("KST 10:05는 직전 발표 08:00 기반 파라미터를 생성해야 한다")
    void KST_1005는_직전_0800_발표_기반이어야_한다() {
        var at = LocalDateTime.of(2025, 9, 24, 10, 5).atZone(KST).toInstant();
        Map<String, String> params = new KmaRequestBuilder().toParams(37.5665, 126.9780, at);

        assertThat(params.get("base_date")).isEqualTo("20250924");
        assertThat(params.get("base_time")).isEqualTo("0800");
        assertThat(params).containsKeys("nx", "ny", "numOfRows", "dataType");
    }

    @Test
    @DisplayName("발표 경계 14:10은 base_time=1400 이어야 한다")
    void 발표_경계_1410은_base_time_1400이어야_한다() {
        var at = LocalDateTime.of(2025, 9, 24, 14, 10).atZone(KST).toInstant();
        Map<String, String> params = new KmaRequestBuilder().toParams(35.1796, 129.0756, at);

        assertThat(params.get("base_date")).isEqualTo("20250924");
        assertThat(params.get("base_time")).isEqualTo("1400");
    }

    @Test
    @DisplayName("격자 변환(nx, ny) 파라미터가 포함되어야 한다")
    void 격자_변환_파라미터가_포함되어야_한다() {
        var at = LocalDateTime.of(2025, 9, 24, 9, 0).atZone(KST).toInstant();
        Map<String, String> params = new KmaRequestBuilder().toParams(37.5665, 126.9780, at);

        assertThat(params.get("nx")).isNotBlank();
        assertThat(params.get("ny")).isNotBlank();
    }
}