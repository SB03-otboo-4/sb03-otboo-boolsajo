package com.sprint.otboo.weather.change;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.change")
public class WeatherChangeThresholdProperties {

    private double tempJumpC = 5.0;
    private double popJumpPct = 30.0;

    public double getTempJumpC() { return tempJumpC; }
    public void setTempJumpC(double v) { this.tempJumpC = v; }
    public double getPopJumpPct() { return popJumpPct; }
    public void setPopJumpPct(double v) { this.popJumpPct = v; }
}
