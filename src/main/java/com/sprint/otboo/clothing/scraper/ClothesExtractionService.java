package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
