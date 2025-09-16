package com.sprint.otboo.common.exception.weather;

import com.sprint.otboo.common.exception.ErrorCode;

public class WeatherNotFoundException extends WeatherException{

    public WeatherNotFoundException() { super(ErrorCode.WEATHER_LOCATION_NOT_FOUND); }
}
