package com.sprint.otboo.weather.integration.kma;

import com.sprint.otboo.common.util.KmaGridConverter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KmaRequestBuilder {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int[] ANNOUNCE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final WeatherKmaProperties props;

    public KmaRequestBuilder(WeatherKmaProperties props) {
        this.props = props;
    }

    /**
     * @param lat 위도
     * @param lon 경도
     * @param at  기준 시각(UTC Instant). null이면 now().
     */
    public Map<String, String> toParams(double lat, double lon, Instant at) {
        Instant baseInstant = (at == null) ? Instant.now() : at;
        LocalDateTime kst = LocalDateTime.ofInstant(baseInstant, KST);

        // 1) 발표시각 계산
        LocalDateTime baseDateTime = calcBaseDateTime(kst);

        // 2) 포맷 (yyyyMMdd, HHmm)
        String baseDate = baseDateTime.toLocalDate().format(DATE_FMT);
        String baseTime = String.format("%02d00", baseDateTime.getHour());

        // 3) 격자 변환(정적 유틸)
        KmaGridConverter.XY xy = KmaGridConverter.toXY(lat, lon);
        String nx = String.valueOf(xy.x());
        String ny = String.valueOf(xy.y());

        // 4) 파라미터 조립
        Map<String, String> params = new LinkedHashMap<>();
        params.put("base_date", baseDate);
        params.put("base_time", baseTime);
        params.put("nx", nx);
        params.put("ny", ny);
        params.put("pageNo", "1");
        // ▼ 한 번에 많이 받도록 상향 (props가 더 크면 그 값 사용)
        int rows = Math.max(1000, props.getNumOfRows());
        params.put("numOfRows", String.valueOf(rows));
        params.put("dataType", props.getDataType());
        return params;
    }

    /**
     * 발표시각 규칙:
     * - 기준 시각이 발표시각이고 분 >= 10 → 그 발표시각
     * - 그 외 → 직전 발표시각
     * - 00:00~01:59는 전날 23:00
     */
    static LocalDateTime calcBaseDateTime(LocalDateTime kst) {
        int hour = kst.getHour();
        int minute = kst.getMinute();

        if (isAnnounceHour(hour) && minute >= 10) {
            return kst.withMinute(0).withSecond(0).withNano(0);
        }

        int prev = previousAnnounceHour(hour);
        LocalDate baseDate = kst.toLocalDate();
        if (prev > hour) { // 자정 경계 처리
            baseDate = baseDate.minus(1, ChronoUnit.DAYS);
        }
        return LocalDateTime.of(baseDate.getYear(), baseDate.getMonth(), baseDate.getDayOfMonth(), prev, 0);
    }

    private static boolean isAnnounceHour(int h) {
        for (int ah : ANNOUNCE_HOURS) {
            if (ah == h) return true;
        }
        return false;
    }

    private static int previousAnnounceHour(int h) {
        int prev = 23;
        for (int ah : ANNOUNCE_HOURS) {
            if (ah <= h) prev = ah;
            else break;
        }
        if (h < 2) prev = 23;
        return prev;
    }
}