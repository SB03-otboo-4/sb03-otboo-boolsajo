package com.sprint.otboo.weather.dto.data;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import java.time.Instant;
import java.util.UUID;

public record WeatherDto(
    UUID weatherId,                 // 날씨 ID
    Instant forecastedAt,           // 예보된 시간
    Instant forecastAt,             // 예보 대상 시간
    WeatherLocationResponse location, // 위치 (위도/경도/x/y/지명)
    String skyStatus,               // CLEAR / MOSTLY_CLOUDY / CLOUDY
    PrecipitationDto precipitation, // 강수 (type/amount/probability)
    HumidityDto humidity,           // 습도 (current/comparedToDayBefore)
    TemperatureDto temperature,     // 온도 (current/comparedToDayBefore/min/max)
    WindSpeedDto windSpeed          // 풍속 (speed/asWord)
) {}