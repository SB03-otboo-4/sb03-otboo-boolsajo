package com.sprint.otboo.weather.integration.owm.mapper;

import com.sprint.otboo.weather.entity.WindStrength;

/**
 * 풍속(m/s)을 3단계로 변환:
 *  - WEAK     : v < 4.0
 *  - MODERATE : 4.0 ≤ v < 9.0
 *  - STRONG   : 9.0 ≤ v
 *
 * 기준은 보퍼트 스케일을 단순화한 경험적 컷오프.
 * 프로젝트 요구에 따라 임계값은 자유롭게 조정 가능.
 */
public class DefaultWindStrengthResolver implements WindStrengthResolver {

    @Override
    public WindStrength resolve(Double speedMs) {
        double v = speedMs == null ? 0d : speedMs;
        if (v < 4.0) return WindStrength.WEAK;
        if (v < 9.0) return WindStrength.MODERATE;
        return WindStrength.STRONG;
    }
}
