package com.sprint.otboo.weather.integration.kma.dto;

public class KmaForecastItem {

    private String category;
    private String fcstDate;
    private String fcstTime;
    private String fcstValue;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFcstDate() { return fcstDate; }
    public void setFcstDate(String fcstDate) { this.fcstDate = fcstDate; }
    public String getFcstTime() { return fcstTime; }
    public void setFcstTime(String fcstTime) { this.fcstTime = fcstTime; }
    public String getFcstValue() { return fcstValue; }
    public void setFcstValue(String fcstValue) { this.fcstValue = fcstValue; }
}
