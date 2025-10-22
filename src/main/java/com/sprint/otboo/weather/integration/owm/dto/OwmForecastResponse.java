package com.sprint.otboo.weather.integration.owm.dto;

import java.util.List;

public record OwmForecastResponse(
    City city,
    List<Item> list
) {
    public record City(
        int id, String name, Coord coord, String country, int population, int timezone, long sunrise
    ) {}
    public record Coord(double lat, double lon) {}
    public record Item(
        long dt, Main main, List<Weather> weather, Clouds clouds, Wind wind, Double pop,
        Rain rain, Snow snow, String dt_txt
    ) {}
    public record Main(Double temp, Double temp_min, Double temp_max, Integer humidity, Double pressure) {}
    public record Weather(Integer id, String main, String description, String icon) {}
    public record Clouds(Integer all) {}
    public record Wind(Double speed, Integer deg, Double gust) {}
    public record Rain(Double _3h) {}
    public record Snow(Double _3h) {}
}
