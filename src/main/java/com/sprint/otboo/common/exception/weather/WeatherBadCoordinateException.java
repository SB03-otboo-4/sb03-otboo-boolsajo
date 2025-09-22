package com.sprint.otboo.common.exception.weather;

import com.sprint.otboo.common.exception.ErrorCode;

public class WeatherBadCoordinateException extends WeatherException {

    public WeatherBadCoordinateException() { super(ErrorCode.WEATHER_BAD_COORDINATE); }
}
