package com.sprint.otboo.weather.integration.kma.dto;

import java.util.ArrayList;
import java.util.List;

public class KmaForecastResponse {
    private String resultCode;
    private List<KmaForecastItem> items = new ArrayList<>();

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public List<KmaForecastItem> getItems() { return items; }
    public void setItems(List<KmaForecastItem> items) { this.items = items; }
}