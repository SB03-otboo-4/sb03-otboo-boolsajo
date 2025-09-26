package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.entity.attribute.Thickness;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
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

            // 사이트에서 카테고리 텍스트 추출 (예시: breadcrumb, meta tag 등)
            String category = doc.selectFirst(".breadcrumb a").text();

            // 카테고리 → ClothesType 매핑
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);

            List<ClothesAttributeDto> attributes = new ArrayList<>();

            // 색상
            doc.select(".product-color span").forEach(el ->
                attributes.add(new ClothesAttributeDto(UUID.randomUUID(), el.text()))
            );

            // 사이즈
            doc.select(".product-size span").forEach(el ->
                attributes.add(new ClothesAttributeDto(UUID.randomUUID(), el.text()))
            );

            // 소재
            doc.select(".product-material span").forEach(el ->
                attributes.add(new ClothesAttributeDto(UUID.randomUUID(), el.text()))
            );

            // 계절
            doc.select(".product-season span").forEach(el -> {
                Season season = Season.fromString(el.text());
                if (season != null) {
                    attributes.add(new ClothesAttributeDto(UUID.randomUUID(), season.name()));
                }
            });

            // 두께
            doc.select(".product-thickness span").forEach(el -> {
                Thickness thickness = Thickness.fromString(el.text());
                if (thickness != null) {
                    attributes.add(new ClothesAttributeDto(UUID.randomUUID(), thickness.name()));
                }
            });

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