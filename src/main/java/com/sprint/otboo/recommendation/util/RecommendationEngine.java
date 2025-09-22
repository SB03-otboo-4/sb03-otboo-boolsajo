package com.sprint.otboo.recommendation.util;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.weather.entity.Weather;
import java.util.List;

public interface RecommendationEngine {

    /**
     * 계절 및 세부 온도 범주를 기반으로 의상 추천
     *
     * @param clothes 사용자 의상 목록
     * @param perceivedTemp 체감 온도
     * @param weather 날씨 정보
     * @return 추천 의상 리스트
     */
    List<Clothes> recommend(List<Clothes> clothes, double perceivedTemp, Weather weather);
}