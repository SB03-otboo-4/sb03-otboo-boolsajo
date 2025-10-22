package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.data.MallRule;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import com.sprint.otboo.common.storage.FileStorageService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>다양한 쇼핑몰 상품 페이지에서 의상 정보를 추출하는 {@link ClothesExtractor} 통합 구현체</p>
 *
 * <p>지원 쇼핑몰:</p>
 * <ul>
 *     <li>Musinsa, Zigzag, 29CM, Hiver, Ably, 4910, WConcept, SSG, Brandi</li>
 * </ul>
 *
 * <p>동작 방식:</p>
 * <ol>
 *     <li>각 쇼핑몰 URL별 CSS select</li>
 *     <li>헤더 정보 기반 HTML 문서 로드</li>
 *     <li>상품명, 이미지, 카테고리 추출</li>
 *     <li>필요시 외부 이미지 다운로드 후 내부 저장소 업로드</li>
 *     <li>카테고리/상품명 기반 ClothesType 결정</li>
 *     <li>HTML/텍스트 기반 속성 추출</li>
 *     <li>DB 정의에 따라 selectable 값 보정 후 DTO 변환</li>
 *     <li>{@link ClothesDto} 반환</li>
 * </ol>
 */
@Slf4j
@Component
public class GenericExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;
    private final FileStorageService fileStorageService;

    public GenericExtractor(
        ClothesAttributeExtractor extractor,
        ClothesAttributeDefRepository defRepository,
        // 로컬 저장소로 전환 시 해당 빈으로 변경 필요
//        @Qualifier("localFileStorageService") FileStorageService fileStorageService
        // 저장소 S3로 전환 시 해당 빈으로 변경 필요
        @Qualifier("clothingImageStorageService") FileStorageService fileStorageService
    ) {
        this.extractor = extractor;
        this.defRepository = defRepository;
        this.fileStorageService = fileStorageService;
    }

    // 쇼핑몰별 CSS selector 및 처리 규칙 정의
    private final List<MallRule> rules = List.of(
        new MallRule("musinsa.com",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb a",
            false,
            Map.of()
        ),
        new MallRule("zigzag.kr",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb li a",
            false,
            Map.of("User-Agent", "Mozilla/5.0")
        ),
        new MallRule("29cm.co.kr",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb a",
            false,
            Map.of("User-Agent", "Mozilla/5.0")
        ),
        new MallRule("hiver.co.kr",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb li a",
            true,
            Map.of("User-Agent", "Mozilla/5.0")
        ),
        new MallRule("a-bly.com",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb a",
            true,
            Map.of(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Referer", "https://m.a-bly.com"
            )
        ),
        new MallRule("4910.kr",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb a",
            true,
            Map.of()
        ),
        new MallRule("wconcept.co.kr",
            // W Concept은 description 사용, title 사용 시 [ W Concept ] 추출
            "meta[name=description]",
            "meta[property=og:image]",
            ".breadcrumb li a",
            false,
            Map.of()
        ),
        new MallRule(
            "ssg.com",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb li a",
            false,
            // 요청 시 헤더
            Map.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        ),
        new MallRule("brandi.co.kr",
            "meta[property=og:title]",
            "meta[property=og:image]",
            ".breadcrumb li a",
            true,
            Map.of("Referer", "https://www.brandi.co.kr")
        )
    );

    /**
     * 해당 URL이 지원되는 쇼핑몰인지 확인
     *
     * @param url 요청 URL
     * @return 지원 여부
     */
    @Override
    public boolean supports(String url) {
        return rules.stream().anyMatch(r -> url.contains(r.host()));
    }

    /**
     * URL에서 의상 정보 추출
     *
     * @param url 상품 URL
     * @return 추출된 {@link ClothesDto}
     * @throws ClothesExtractionException 추출 실패 시
     */
    @Override
    public ClothesDto extract(String url) {
        // 1. URL에 맞는 규칙 선택
        MallRule rule = rules.stream()
            .filter(r -> url.contains(r.host()))
            .findFirst()
            .orElseThrow(() -> new ClothesExtractionException("지원하지 않는 URL"));

        try {
            // 2. HTML 문서 로드
            Connection connection = Jsoup.connect(url);
            rule.headers().forEach(connection::header);
            Document doc = connection.get();

            // 3. 기본 정보 추출: 상품명, ImageUrl, 카테고리
            String name = extractor.getAttrOrDefault(doc, rule.nameSelector(), "content", "이름없음");
            String externalImageUrl = extractor.getAttrOrDefault(doc, rule.imageSelector(), "content", "");
            String category = extractor.getLastBreadcrumbOrDefault(doc, rule.categorySelector(), "ETC");

            log.info("[{}] 상품명: {}", rule.host(), name);
            log.info("[{}] ImageURL: {}", rule.host(), externalImageUrl);

            // 4. ImageUrl 다운로드 → 내부 저장소 업로드( 필요 시 )
            String imageUrl = "";
            if (externalImageUrl != null && !externalImageUrl.isBlank() && rule.saveImage()) {
                try {
                    MultipartFile downloaded = extractor.downloadImageAsMultipartFile(externalImageUrl);
                    imageUrl = fileStorageService.upload(downloaded);
                    log.info("[{}] 이미지 서버 저장 완료, 내부 URL: {}", rule.host(), imageUrl);
                } catch (IOException e) {
                    log.warn("[{}] 이미지 다운로드/업로드 실패, URL 비워둠: {}", rule.host(), e.getMessage());
                }
            } else {
                imageUrl = externalImageUrl;
            }

            // 5. ClothesType 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }
            log.info("[{}] ClothesType 결정: {}", rule.host(), type);

            // 6. 속성 추출
            List<ClothesAttributeExtractor.Attribute> attributes = extractor.extractAttributes(doc, name);

            // 7. DTO 변환: 추출 속성을 DB 정의 기반 selectable 값 매핑
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES
                        .getOrDefault(attr.type(), List.of(attr.type().name()));

                    for (String dbName : dbNames) {
                        var defOpt = defRepository.findByName(dbName);
                        if (defOpt.isPresent()) {
                            var def = defOpt.get();
                            String matchedValue = extractor.matchSelectableValue(attr.value(), def.getSelectValues());
                            log.info("[{}] 속성 매핑: {} -> {} (DB 정의: {})", rule.host(), attr.value(), matchedValue, dbName);
                            return Stream.of(new ClothesAttributeDto(def.getId(), matchedValue));
                        }
                    }
                    log.warn("DB 정의 없는 속성 건너뜀: {}", attr.type());
                    return Stream.empty();
                })
                .toList();

            // 8. DTO 반환
            return new ClothesDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                imageUrl,
                type,
                finalAttributes
            );

        } catch (IOException e) {
            throw new ClothesExtractionException(rule.host() + " URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}