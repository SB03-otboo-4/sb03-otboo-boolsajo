package com.sprint.otboo.weather.integration.kma;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KMA 요청 파라미터 조립 유틸 (최소 구현: 테스트 통과 목적)
 * - base_date/base_time: KST 기준, 발표시각(02,05,08,11,14,17,20,23) 규칙 적용
 * - nx, ny: 현재는 최소 구현(반올림 기반) — 추후 KmaGridConverter 연동
 */
public class KmaRequestBuilder {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int[] ANNOUNCE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    private final WeatherKmaProperties props;

    public KmaRequestBuilder() {
        // 테스트 용도: 기본 프로퍼티로 생성 가능하게
        this.props = new WeatherKmaProperties();
    }

    public KmaRequestBuilder(WeatherKmaProperties props) {
        this.props = props;
    }

    /**
     * @param lat 위도
     * @param lon 경도
     * @param at  기준시각(UTC Instant). null이면 now()
     */
    public Map<String, String> toParams(double lat, double lon, Instant at) {
        Instant baseInstant = (at == null) ? Instant.now() : at;
        LocalDateTime kst = LocalDateTime.ofInstant(baseInstant, KST);

        // 1) 발표시각 계산
        LocalDateTime baseDt = calcBaseDateTime(kst);

        // 2) 날짜/시간 포맷 (yyyyMMdd, HHmm)
        String baseDate = baseDt.toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String baseTime = String.format("%02d00", baseDt.getHour());

        // 3) 격자(nx, ny) — 최소 구현: 반올림 기반 (테스트는 "존재"만 검사)
        //    TODO: 실제 변환으로 교체: com.sprint.otboo.common.util.KmaGridConverter.toXY(lat, lon)
        int nx = (int) Math.round(lon);
        int ny = (int) Math.round(lat);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("base_date", baseDate);
        params.put("base_time", baseTime);
        params.put("nx", String.valueOf(nx));
        params.put("ny", String.valueOf(ny));
        params.put("numOfRows", String.valueOf(props.getNumOfRows()));
        params.put("dataType", props.getDataType());
        return params;
    }

    /**
     * 발표시각 규칙:
     * - 기준 시각이 정확히 발표시각이고 분 >= 10인 경우 → 그 발표시각
     * - 그 외 → 직전 발표시각
     * - 00:00~01:59는 전날 23:00
     */
    static LocalDateTime calcBaseDateTime(LocalDateTime kst) {
        int hour = kst.getHour();
        int minute = kst.getMinute();

        // 발표시각인지 확인
        boolean isAnnounceHour = isAnnounceHour(hour);
        if (isAnnounceHour && minute >= 10) {
            return kst.withMinute(0).withSecond(0).withNano(0);
        }

        // 직전 발표시각 찾기
        int prev = previousAnnounceHour(hour);
        LocalDate baseDate = kst.toLocalDate();
        if (prev > hour) { // 자정 경계: 00~01시는 전날 23시
            baseDate = baseDate.minus(1, ChronoUnit.DAYS);
        }
        return LocalDateTime.of(baseDate.getYear(), baseDate.getMonth(), baseDate.getDayOfMonth(), prev, 0);
    }

    private static boolean isAnnounceHour(int h) {
        for (int ah : ANNOUNCE_HOURS) if (ah == h) return true;
        return false;
    }

    private static int previousAnnounceHour(int h) {
        int prev = 23;
        for (int ah : ANNOUNCE_HOURS) {
            if (ah <= h) prev = ah;
            else break;
        }
        // 0~1시 구간은 prev가 23으로 남도록
        if (h < 2) prev = 23;
        return prev;
    }
}