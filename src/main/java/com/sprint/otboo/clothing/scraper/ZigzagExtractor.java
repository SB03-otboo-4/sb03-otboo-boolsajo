package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ZigzagExtractor implements ClothesExtractor {

    @Override
    public boolean supports(String url) {
        return url.contains("zigzag.kr");
    }

    @Override
    public ClothesDto extract(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String name = doc.selectFirst("meta[property=og:title]").attr("content");
            String imageUrl = doc.selectFirst("meta[property=og:image]").attr("content");

            ClothesType type = ClothesType.TOP;
            List<ClothesAttributeDto> attributes = List.of(
                new ClothesAttributeDto(UUID.randomUUID(), "블루")
            );

            return new ClothesDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                imageUrl,
                type,
                attributes
            );
        } catch (IOException e) {
            throw new ClothesExtractionException("지그재그 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}