package com.sprint.otboo.weather.integration.kma;

import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import java.util.Map;

public interface KmaShortTermForecastClient {

    KmaForecastResponse getUltraSrtFcst(Map<String, String> params);
}