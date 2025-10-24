package com.sprint.otboo.common.exception.weather;

import com.sprint.otboo.common.exception.ErrorCode;
import java.util.UUID;

public class WeatherNotFoundException extends WeatherException {

    public static WeatherNotFoundException withId(UUID weatherId) {
        WeatherNotFoundException exception = new WeatherNotFoundException();
        exception.addDetail("id", weatherId);
        return exception;
    }
  
    public WeatherNotFoundException() { super(ErrorCode.WEATHER_LOCATION_NOT_FOUND); }
}
