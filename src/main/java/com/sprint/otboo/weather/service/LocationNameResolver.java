package com.sprint.otboo.weather.service;

import java.util.List;

public interface LocationNameResolver {

    List<String> resolve(double longitude, double latitude);
}
