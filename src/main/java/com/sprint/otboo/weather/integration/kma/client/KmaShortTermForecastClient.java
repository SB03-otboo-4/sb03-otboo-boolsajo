package com.sprint.otboo.weather.integration.kma.client;

import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import java.util.Map;

public interface KmaShortTermForecastClient {

    KmaForecastResponse getVilageFcst(Map<String, String> params);
}