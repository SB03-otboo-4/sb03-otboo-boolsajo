package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * 29CM(29cm.co.kr) 사이트에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체.
 *
 * <p>주요 기능:
 * <ul>
 *   <li>URL 지원 여부 판단 (supports)</li>
 *   <li>상품명, 이미지, 카테고리 등 기본 정보 추출</li>
 *   <li>카테고리 기반 {@link ClothesType} 결정</li>
 *   <li>속성 추출 및 DB 기반 selectable 값 보정</li>
 *   <li>최종 {@link ClothesDto} 반환</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwentynineCExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;

    @Override
    public boolean supports(String url) {
        return url.contains("29cm.co.kr");
    }

    @Override
    public ClothesDto extract(String url) {
        try {
            // 1. HTML 문서 로드
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

            // 2. 기본 정보 추출
            String name = getAttrOrDefault(doc, "meta[property=og:title]", "content", "이름없음");
            String imageUrl = getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = getLastBreadcrumbOrDefault(doc, ".breadcrumb li a", "ETC");

            log.info("상품명 추출: {}", name);
            log.info("이미지 URL 추출: {}", imageUrl);

            // 3. ClothesType 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }
            log.info("ClothesType 결정: {}", type);

            // 4. 속성 추출
            List<ClothesAttributeExtractor.Attribute> attributes = extractor.extractAttributes(doc, name);

            // 5. DB 기반 selectable 값 보정 후 DTO 변환
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES.getOrDefault(attr.type(), List.of(attr.type().name()));
                    for (String dbName : dbNames) {
                        var defOpt = defRepository.findByName(dbName);
                        if (defOpt.isPresent()) {
                            var def = defOpt.get();
                            String matchedValue = matchSelectableValue(attr.value(), def.getSelectValues());
                            log.info("최종 DTO 매핑: {} -> {} (DB 속성: {})", attr.type(), matchedValue, dbName);
                            return Stream.of(new ClothesAttributeDto(def.getId(), matchedValue));
                        }
                    }
                    log.warn("DB 정의 없는 속성 건너뜀: {}", attr.type());
                    return Stream.empty();
                })
                .toList();

            // 6. DTO 반환
            return new ClothesDto(UUID.randomUUID(), UUID.randomUUID(), name, imageUrl, type, finalAttributes);

        } catch (IOException e) {
            throw new ClothesExtractionException("29CM URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }

    private String matchSelectableValue(String value, String selectableValues) {
        if (value == null || value.isBlank()) return value;

        List<String> values = selectableValues == null || selectableValues.isBlank()
            ? Collections.emptyList()
            : Arrays.asList(selectableValues.split(","));

        return values.stream()
            .filter(sel -> sel.equalsIgnoreCase(value))
            .findFirst()
            .orElseGet(() -> {
                log.warn("SelectableValues에 없는 값, 그대로 사용: {}", value);
                return value;
            });
    }

    private String getLastBreadcrumbOrDefault(Document doc, String cssQuery, String defaultValue) {
        Elements crumbs = doc.select(cssQuery);
        return crumbs.isEmpty() ? defaultValue : crumbs.last().text();
    }

    private String getAttrOrDefault(Document doc, String cssQuery, String attr, String defaultValue) {
        Element el = doc.selectFirst(cssQuery);
        return el != null ? el.attr(attr) : defaultValue;
    }
}