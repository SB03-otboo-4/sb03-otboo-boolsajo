package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
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
 * 지그재그(zigzag.kr) 사이트에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체.
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
public class ZigzagExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;

    @Override
    public boolean supports(String url) {
        return url.contains("zigzag.kr");
    }

    @Override
    public ClothesDto extract(String url) {
        try {
            // 1. HTML 문서 로드
            Document doc = Jsoup.connect(url).get();

            // 2. 상품명, 이미지 URL, 카테고리 추출
            String name = extractor.getAttrOrDefault(doc, "meta[property=og:title]", "content", "이름없음");
            String imageUrl = extractor.getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = extractor.getLastBreadcrumbOrDefault(doc, ".breadcrumb li a", "ETC");

            log.info("상품명 추출: {}", name);
            log.info("이미지 URL 추출: {}", imageUrl);

            // 3. ClothesType 결정 ( 카테고리 우선, 상품명 보조 )
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }
            log.info("ClothesType 결정: {}", type);

            // 4. 속성 추출
            List<ClothesAttributeExtractor.Attribute> attributes = extractor.extractAttributes(doc, name);

            // 5. DTO 변환: 추출 속성을 DB 정의 기반으로 selectable 값 매핑
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    // DB 칼럼명 후보 가져오기
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES.getOrDefault(attr.type(), List.of(attr.type().name()));

                    // DB 조회 및 selectable 값 매핑
                    for (String dbName : dbNames) {
                        var defOpt = defRepository.findByName(dbName);
                        if (defOpt.isPresent()) {
                            var def = defOpt.get();

                            // 추출값을 DB 정의 selectable 값 기준으로 매칭
                            String matchedValue = extractor.matchSelectableValue(attr.value(), def.getSelectValues());

                            // DTO 반환
                            return Stream.of(new ClothesAttributeDto(def.getId(), matchedValue));
                        }
                    }
                    // DB 정의 없는 속성은 건너뜀
                    log.warn("DB 정의 없는 속성 건너뜀: {}", attr.type());
                    return Stream.empty();
                })
                .toList();

            return new ClothesDto(UUID.randomUUID(), UUID.randomUUID(), name, imageUrl, type, finalAttributes);

        } catch (IOException e) {
            throw new ClothesExtractionException("지그재그 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}