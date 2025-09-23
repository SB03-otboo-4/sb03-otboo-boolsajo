package com.sprint.otboo.weather.integration.kakao.client;

import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;

public interface KakaoLocalClient {

    KakaoCoord2RegioncodeResponse coord2RegionCode(double longitude, double latitude);
}
