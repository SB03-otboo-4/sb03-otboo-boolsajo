package com.sprint.otboo.weather.change;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.Weather;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeatherChangeDetector {

    public List<DetectedChange> detect(Weather older, Weather newer, WeatherChangeThresholdProperties th) {
        List<DetectedChange> out = new ArrayList<>();
        if (older == null || newer == null) return out;

        // 1) PTY 전환
        PrecipitationType p1 = older.getType();
        PrecipitationType p2 = newer.getType();
        if (p1 != null && p2 != null && p1 != p2) {
            HashMap<String, Object> detail = new HashMap<>();
            detail.put("from", p1.name());
            detail.put("to", p2.name());
            out.add(new DetectedChange(WeatherChangeType.PTY_CHANGE, detail));
        }

        // 2) TEMP jump (절대값)
        Double t1 = older.getCurrentC();
        Double t2 = newer.getCurrentC();
        if (t1 != null && t2 != null) {
            double diff = Math.abs(t2 - t1);
            if (diff >= th.getTempJumpC()) {
                HashMap<String, Object> detail = new HashMap<>();
                detail.put("deltaC", t2 - t1);
                detail.put("from", t1);
                detail.put("to", t2);
                out.add(new DetectedChange(WeatherChangeType.TEMP_JUMP, detail));
            }
        }

        // 3) POP jump (증가량 기준)
        Double pop1 = older.getProbability();
        Double pop2 = newer.getProbability();
        if (pop1 != null && pop2 != null) {
            double inc = pop2 - pop1;
            if (inc >= th.getPopJumpPct()) {
                HashMap<String, Object> detail = new HashMap<>();
                detail.put("deltaPct", inc);
                detail.put("from", pop1);
                detail.put("to", pop2);
                out.add(new DetectedChange(WeatherChangeType.POP_JUMP, detail));
            }
        }

        return out;
    }
}
