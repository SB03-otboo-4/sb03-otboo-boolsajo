package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import java.util.List;

public interface KmaForecastMapper {

    SkyStatus mapSky(String skyCode);
    PrecipitationType mapPrecipitation(String ptyCode);
    int extractInt(List<KmaForecastItem> items, String category, int defaultValue);
    Slot toSlot(List<KmaForecastItem> items, String fcstDate, String fcstTime);

    class Slot {
        private final String fcstDate;
        private final String fcstTime;
        private final SkyStatus sky;
        private final PrecipitationType precipitation;
        private final int temperature;
        private final int humidity;
        private final int precipitationProbability;

        public Slot(String fcstDate, String fcstTime, SkyStatus sky,
            PrecipitationType precipitation, int temperature,
            int humidity, int precipitationProbability) {
            this.fcstDate = fcstDate;
            this.fcstTime = fcstTime;
            this.sky = sky;
            this.precipitation = precipitation;
            this.temperature = temperature;
            this.humidity = humidity;
            this.precipitationProbability = precipitationProbability;
        }

        public String getFcstDate() { return fcstDate; }
        public String getFcstTime() { return fcstTime; }
        public SkyStatus getSky() { return sky; }
        public PrecipitationType getPrecipitation() { return precipitation; }
        public int getTemperature() { return temperature; }
        public int getHumidity() { return humidity; }
        public int getPrecipitationProbability() { return precipitationProbability; }
    }
}
