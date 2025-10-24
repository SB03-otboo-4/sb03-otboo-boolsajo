package com.sprint.otboo.weather.integration.owm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OwmForecastResponse(
    @JsonProperty("list") List<Item> list,
    @JsonProperty("city") City city
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        @JsonProperty("dt") long dt,
        @JsonProperty("dt_txt") String dtTxt,
        @JsonProperty("main") Main main,
        @JsonProperty("wind") Wind wind,
        @JsonProperty("clouds") Clouds clouds,
        @JsonProperty("rain") Rain rain,
        @JsonProperty("snow") Snow snow,
        @JsonProperty("pop") Double pop,
        @JsonProperty("weather") List<Weather> weather,
        @JsonProperty("sys") Sys sys
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Main(
        @JsonProperty("temp") Double temp,
        @JsonProperty("feels_like") Double feelsLike,
        @JsonProperty("temp_min") Double tempMin,
        @JsonProperty("temp_max") Double tempMax,
        @JsonProperty("pressure") Integer pressure,
        @JsonProperty("humidity") Integer humidity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Wind(
        @JsonProperty("speed") Double speed,
        @JsonProperty("deg") Integer deg,
        @JsonProperty("gust") Double gust
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Clouds(
        @JsonProperty("all") Integer all   // %
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rain(
        @JsonProperty("3h") Double _3h     // mm
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snow(
        @JsonProperty("3h") Double _3h     // mm
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Weather(
        @JsonProperty("id") Integer id,
        @JsonProperty("main") String main,
        @JsonProperty("description") String description,
        @JsonProperty("icon") String icon
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sys(
        @JsonProperty("pod") String pod // "d"/"n"
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(
        @JsonProperty("timezone") Integer timezone // seconds shift from UTC
    ) {}
}
