package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesDto;

/**
 * 의상 정보 추출기 인터페이스
 *
 * <p>구현체는 특정 쇼핑몰이나 URL 패턴에 따라 의상 정보를 추출할 수 있어야 함</p>
 */
public interface ClothesExtractor {

    // 해당 URL 지원 여부 확인
    boolean supports(String url);

    // URL 기반 의상 정보 추출
    ClothesDto extract(String url);
}
