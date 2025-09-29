package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * W Concept 상품 페이지에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체
 *
 * <p>지원 URL: wconcept.co.kr</p>
 *
 * <p>추출 방식:</p>
 * <ul>
 *   <li>JSON-LD 스크립트 기반 속성 추출</li>
 *   <li>HTML 태그 기반 속성 추출</li>
 *   <li>상품명 및 텍스트 기반 간접 속성 추출</li>
 * </ul>
 *
 * <p>추출 속성:</p>
 * <ul>
 *   <li>{@link AttributeType#COLOR} 색상</li>
 *   <li>{@link AttributeType#SIZE} 사이즈</li>
 *   <li>{@link AttributeType#MATERIAL} 소재</li>
 *   <li>{@link AttributeType#SEASON} 계절</li>
 *   <li>{@link AttributeType#THICKNESS} 두께</li>
 * </ul>
 *
 * <p>속성 값 매칭 시 DB 정의에 따라 선택값 보정 수행</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WConceptExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;

    @Override
    public boolean supports(String url) {
        return url.contains("wconcept.co.kr");
    }

    @Override
    public ClothesDto extract(String url) {
        try {
            // 1. HTML 문서 로드
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get();

            // 2. 상품명, 이미지 URL, 카테고리 추출
            // title이 아닌 description으로 조회하는 이유는, 해당 사이트는 Title 조회 시 [ W Concept ] 가 추출됩니다
            String name = extractor.getAttrOrDefault(doc, "meta[name=description]", "content", "이름없음");
            String imageUrl = extractor.getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = extractor.getLastBreadcrumbOrDefault(doc, ".breadcrumb li a", "ETC");

            log.info("W Concept 상품명: {}", name);
            log.info("W Concept 이미지 URL: {}", imageUrl);

            // 3. ClothesType 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }
            log.info("W Concept ClothesType 결정: {}", type);

            // 4. 속성 추출
            List<ClothesAttributeExtractor.Attribute> attributes = extractor.extractAttributes(doc, name);

            // 5. DTO 변환: DB 정의 기반 selectable 값 매핑
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES.getOrDefault(
                        attr.type(), List.of(attr.type().name())
                    );

                    for (String dbName : dbNames) {
                        var defOpt = defRepository.findByName(dbName);
                        if (defOpt.isPresent()) {
                            var def = defOpt.get();
                            String matchedValue = extractor.matchSelectableValue(attr.value(), def.getSelectValues());
                            return Stream.of(new ClothesAttributeDto(def.getId(), matchedValue));
                        }
                    }
                    log.warn("W Concept DB 정의 없는 속성 건너뜀: {}", attr.type());
                    return Stream.empty();
                })
                .toList();

            // 6. DTO 반환
            return new ClothesDto(UUID.randomUUID(), UUID.randomUUID(), name, imageUrl, type, finalAttributes);

        } catch (IOException e) {
            throw new ClothesExtractionException("W Concept URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}