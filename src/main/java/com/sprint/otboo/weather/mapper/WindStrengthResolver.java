package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.entity.WindStrength;

@FunctionalInterface
public interface WindStrengthResolver {
    WindStrength resolve(Double speedMs);
}
