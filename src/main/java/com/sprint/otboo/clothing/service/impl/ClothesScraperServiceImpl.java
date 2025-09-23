package com.sprint.otboo.clothing.service.impl;

import com.sprint.otboo.clothing.dto.data.ScrapedClothesDto;
import com.sprint.otboo.clothing.scraper.ClothesScraper;
import com.sprint.otboo.clothing.service.ClothesScraperService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClothesScraperServiceImpl implements ClothesScraperService {

    private final List<ClothesScraper> scrapers;

    @Override
    public ScrapedClothesDto extractByUrl(String url) {
        return scrapers.stream()
            .filter(s -> s.supports(url))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 사이트 URL"))
            .extract(url);
    }
}
