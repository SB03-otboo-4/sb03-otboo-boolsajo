package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 의상 정보 추출 서비스
 *
 * <p>의상 구매 링크(URL)를 기반으로 적절한 {@link ClothesExtractor}를 선택하여
 * 의상 정보를 추출하는 역할을 담당</p>
 *
 * <ul>
 *   <li>등록된 여러 {@link ClothesExtractor} 중 URL을 지원하는 구현체를 탐색</li>
 *   <li>첫 번째로 매칭되는 추출기를 통해 {@link ClothesDto} 생성</li>
 *   <li>지원하지 않는 사이트의 경우 {@link ClothesExtractionException} 발생</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ClothesExtractionService {

    private final List<ClothesExtractor> extractors;

    public ClothesDto extractByUrl(String url) {
        return extractors.stream()
            .filter(e -> e.supports(url))
            .findFirst()
            .map(e -> e.extract(url))
            .orElseThrow(() -> new ClothesExtractionException("지원하지 않는 사이트 URL입니다."));
    }
}
