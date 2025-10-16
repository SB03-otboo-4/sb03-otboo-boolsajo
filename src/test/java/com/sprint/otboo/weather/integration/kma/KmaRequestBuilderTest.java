package com.sprint.otboo.weather.integration.kma;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KmaRequestBuilder 테스트")
class KmaRequestBuilderTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KmaRequestBuilder newBuilder() {
        // enabled=false 이면 authKey 없어도 통과
        WeatherKmaProperties props = new WeatherKmaProperties(
            null,          // baseUrl
            null,          // vilageFcstPath
            null,          // authKey
            3000,          // connectTimeoutMs
            5000,          // readTimeoutMs
            3,             // retryMaxAttempts
            300,           // retryBackoffMs
            1000,          // numOfRows
            "JSON",        // dataType
            false          // enabled
        );
        return new KmaRequestBuilder(props);
    }

    @Test
    void KST_1005는_직전_0800_발표_기반이어야_한다() {
        Instant at = LocalDateTime.of(2025, 9, 24, 10, 5).atZone(KST).toInstant();
        Map<String, String> params = newBuilder().toParams(37.5665, 126.9780, at);

        assertThat(params.get("base_date")).isEqualTo("20250924");
        assertThat(params.get("base_time")).isEqualTo("0800");
        assertThat(params).containsKeys("nx", "ny", "numOfRows", "dataType");
    }

    @Test
    void 발표_경계_1410은_base_time_1400이어야_한다() {
        Instant at = LocalDateTime.of(2025, 9, 24, 14, 10).atZone(KST).toInstant();
        Map<String, String> params = newBuilder().toParams(35.1796, 129.0756, at);

        assertThat(params.get("base_date")).isEqualTo("20250924");
        assertThat(params.get("base_time")).isEqualTo("1400");
    }

    @Test
    void 격자_변환_파라미터가_포함되어야_한다() {
        Instant at = LocalDateTime.of(2025, 9, 24, 9, 0).atZone(KST).toInstant();
        Map<String, String> params = newBuilder().toParams(37.5665, 126.9780, at);

        assertThat(params.get("nx")).isNotBlank();
        assertThat(params.get("ny")).isNotBlank();
    }

    @Test
    void 발표경계_1410은_1400() {
        Instant at = LocalDateTime.of(2025, 9, 24, 14, 10).atZone(KST).toInstant();
        Map<String, String> p = newBuilder().toParams(37.5665, 126.9780, at);
        assertThat(p.get("base_time")).isEqualTo("1400");
    }

    @Test
    void 자정경계_0005는_전날_2300() {
        Instant at = LocalDateTime.of(2025, 9, 24, 0, 5).atZone(KST).toInstant();
        Map<String, String> p = newBuilder().toParams(37.5665, 126.9780, at);
        assertThat(p.get("base_time")).isEqualTo("2300");
    }
}
