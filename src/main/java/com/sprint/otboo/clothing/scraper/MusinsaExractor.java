package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.ClothesScrapMapper;
import com.sprint.otboo.clothing.mapper.ClothesTypeMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class MusinsaExractor implements ClothesExtractor {

    @Override
    public boolean supports(String url) {
        return url.contains("musinsa.com");
    }

    @Override
    public ClothesDto extract(String url) {
        try {
            Document doc = Jsoup.connect(url).get();

            String name = getAttrOrDefault(doc, "meta[property=og:title]", "content", "이름없음");
            String imageUrl = getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = getLastBreadcrumbOrDefault(doc, "ETC");

            // 카테고리 → ClothesType 매핑
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);

            // breadcrumb 기반 매핑 실패 시 상품명 기반 보조 매핑
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }

            // 의상 속성 추출
            List<ClothesAttributeDto> attributes = new ArrayList<>();

            // 색상
            attributes.addAll(ClothesScrapMapper.mapAttributes(doc.select(".product-color span"), colorStr -> ClothesScrapMapper.normalizeColor(colorStr).name()));

            // 사이즈
            attributes.addAll(ClothesScrapMapper.mapAttributes(doc.select(".product-size span"), sizeStr -> ClothesScrapMapper.normalizeSize(sizeStr).name()));

            // 소재
            attributes.addAll(ClothesScrapMapper.mapAttributes(doc.select(".product-material span"), materialStr -> ClothesScrapMapper.normalizeMaterial(materialStr).name()));

            // 계절
            attributes.addAll(ClothesScrapMapper.mapSeasonAttributes(doc.select(".product-season span")));

            // 두께
            attributes.addAll(ClothesScrapMapper.mapThicknessAttributes(doc.select(".product-thickness span")));


            return new ClothesDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                imageUrl,
                type,
                attributes
            );
        }  catch (IOException e) {
            throw new ClothesExtractionException("무신사 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }

    private String getLastBreadcrumbOrDefault(Document doc, String defaultValue) {
        Elements crumbs = doc.select(".breadcrumb a");
        return crumbs.isEmpty() ? defaultValue : crumbs.last().text();
    }

    private String getAttrOrDefault(Document doc, String cssQuery, String attr, String defaultValue) {
        Element el = doc.selectFirst(cssQuery);
        return el != null ? el.attr(attr) : defaultValue;
    }
}