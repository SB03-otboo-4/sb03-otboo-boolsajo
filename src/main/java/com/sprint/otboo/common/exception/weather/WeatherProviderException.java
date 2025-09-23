package com.sprint.otboo.common.exception.weather;

import com.sprint.otboo.common.exception.ErrorCode;

public class WeatherProviderException extends WeatherException {

    public WeatherProviderException(ErrorCode code) { super(code); }

    public static WeatherProviderException badGateway() {
        return new WeatherProviderException(ErrorCode.WEATHER_PROVIDER_ERROR);
    }

    public static WeatherProviderException tooManyRequests() {
        return new WeatherProviderException(ErrorCode.WEATHER_RATE_LIMIT);
    }

    public static WeatherProviderException timeout() {
        return new WeatherProviderException(ErrorCode.WEATHER_TIMEOUT);
    }
}
