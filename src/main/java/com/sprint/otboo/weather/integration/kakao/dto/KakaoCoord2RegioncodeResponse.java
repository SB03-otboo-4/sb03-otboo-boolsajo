package com.sprint.otboo.weather.integration.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoCoord2RegioncodeResponse(
    Meta meta,
    List<Document> documents
) {
    public record Meta(int total_count) {}
    public record Document(
        String region_type,
        String address_name,
        String region_1depth_name,
        String region_2depth_name,
        String region_3depth_name,
        String region_4depth_name,
        String code,
        double x,
        double y
    ) {}
}
