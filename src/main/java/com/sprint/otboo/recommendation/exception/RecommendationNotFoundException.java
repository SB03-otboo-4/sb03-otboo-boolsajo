package com.sprint.otboo.recommendation.exception;

public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException() {
        super("추천 정보를 찾을 수 없습니다");
    }
}
