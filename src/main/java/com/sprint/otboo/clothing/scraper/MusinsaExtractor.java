package com.sprint.otboo.clothing.scraper;

import static com.sprint.otboo.clothing.util.ClothesAttributeExtractor.TYPE_TO_DB_NAMES;

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
 * 무신사 상품 페이지에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체
 *
 * <p>지원 URL: musinsa.com</p>
 *
 * <p>추출 방식:</p>
 * <ul>
 *   <li>JSON-LD 스크립트 기반 속성 추출</li>
 *   <li>HTML 태그(ul/li/button/span) 기반 속성 추출</li>
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
public class MusinsaExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;

    /**
     * 무신사 URL 지원 여부
     *
     * @param url 요청 URL
     * @return musinsa.com 포함 여부
     */
    @Override
    public boolean supports(String url) {
        return url.contains("musinsa.com");
    }

    /**
     * URL에서 의상 정보 추출
     *
     * <p>동작 흐름:</p>
     * <ol>
     *   <li>JSoup으로 HTML 문서 로드</li>
     *   <li>상품명, 이미지, 카테고리 추출</li>
     *   <li>카테고리/상품명 기반 ClothesType 결정</li>
     *   <li>JSON-LD, HTML, 텍스트 기반 속성 추출</li>
     *   <li>추출 속성을 DB 정의 기반으로 selectable 값 보정</li>
     *   <li>ClothesDto 반환</li>
     * </ol>
     *
     * @param url 무신사 상품 URL
     * @return {@link ClothesDto} 추출된 의상 정보
     * @throws ClothesExtractionException 파싱 실패 시
     */
    @Override
    public ClothesDto extract(String url) {
        try {
            // 1. HTML 문서 로드
            Document doc = Jsoup.connect(url).get();

            // 2. 기본 정보 추출( 상품명, 이미지 URL, 카테고리 )
            String name = extractor.getAttrOrDefault(doc, "meta[property=og:title]", "content", "이름없음");
            String imageUrl = extractor.getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = extractor.getLastBreadcrumbOrDefault(doc, ".breadcrumb a", "ETC");
            log.info("상품명 추출: {}", name);
            log.info("이미지 URL 추출: {}", imageUrl);

            // 3. 의상타입 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            log.info("카테고리 기반 ClothesType 매핑 결과: {}", type);

            // 4. 카테고리 매핑 불가 시 상품명 기반 타입 추정
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
                log.info("상품명 기반 ClothesType 추정 결과: {}", type);
            }

            // 5. 속성 추출
            List<ClothesAttributeExtractor.Attribute> attributes = extractor.extractAttributes(doc, name);

            // 6. Dto 변환
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    // DB 칼럼명 후보 가져오기
                    List<String> dbNames = TYPE_TO_DB_NAMES.getOrDefault(attr.type(), List.of(attr.type().name()));

                    // DB 조회 후 selectable 값 매핑
                    for (String dbName : dbNames) {
                        var defOpt = defRepository.findByName(dbName);
                        if (defOpt.isPresent()) {
                            var def = defOpt.get();
                            String matchedValue = extractor.matchSelectableValue(attr.value(), def.getSelectValues());

                            log.info("최종 DTO 매핑: {} -> {} (DB 속성: {})", attr.type(), matchedValue, dbName);
                            return Stream.of(new ClothesAttributeDto(def.getId(), matchedValue));
                        }
                    }

                    // DB에 정의되지 않은 속성 건너뛰기
                    log.warn("DB에 정의되지 않은 속성 타입, 건너뜀: {}", attr.type());
                    return Stream.empty();
                })
                .toList();

            // 7. 반환
            return new ClothesDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                imageUrl,
                type,
                finalAttributes
            );

        } catch (IOException e) {
            throw new ClothesExtractionException("무신사 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}