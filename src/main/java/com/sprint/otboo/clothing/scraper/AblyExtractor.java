package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor.Attribute;
import com.sprint.otboo.common.storage.FileStorageService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 에이블리 상품 페이지에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체
 *
 * <p>지원 URL: a-bly.com</p>
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
public class AblyExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;
    private final FileStorageService fileStorageService;

    public AblyExtractor(
        ClothesAttributeExtractor extractor,
        ClothesAttributeDefRepository defRepository,
        // 로컬 저장 빈을 한정자로 지정, S3 전환 시 해당 빈을 한정자로 수정 적용 필요
        @Qualifier("localFileStorageService") FileStorageService fileStorageService
    ) {
        this.extractor = extractor;
        this.defRepository = defRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 에이블리 URL 지원 여부
     *
     * @param url 요청 URL
     * @return a-bly.com 포함 여부
     */
    @Override
    public boolean supports(String url) {
        return url.contains("a-bly.com");
    }

    /**
     * URL에서 에이블리 의상 정보 추출
     *
     * <p>동작 흐름:</p>
     * <ol>
     *   <li>JSoup으로 HTML 문서 로드 (User-Agent, Referer 브라우저 흉내)</li>
     *   <li>상품명, 외부 이미지, 카테고리 추출</li>
     *   <li>외부 이미지 다운로드 후 내부 저장소 업로드</li>
     *   <li>카테고리/상품명 기반 ClothesType 결정</li>
     *   <li>HTML/텍스트 기반 속성 추출</li>
     *   <li>추출 속성을 DB 정의 기반으로 selectable 값 보정 후 DTO 변환</li>
     *   <li>ClothesDto 반환</li>
     * </ol>
     *
     * @param url 에이블리 상품 URL
     * @return {@link ClothesDto} 추출된 의상 정보
     * @throws ClothesExtractionException 파싱 실패 시
     */
    @Override
    public ClothesDto extract(String url) {
        try {
            // 1. HTML 문서 로드 (User-Agent, Referer 브라우저 흉내)
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://m.a-bly.com")
                .get();

            // 2. 기본 정보 추출
            String name = extractor.getAttrOrDefault(doc, "meta[property=og:title]", "content", "이름없음");
            String externalImageUrl = extractor.getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = extractor.getLastBreadcrumbOrDefault(doc, ".breadcrumb a", "ETC");

            log.info("상품명: {}", name);
            log.info("외부 이미지 URL: {}", externalImageUrl);

            // 3. 외부 이미지 다운로드 후 내부 저장소 업로드
            String imageUrl = "";
            if (externalImageUrl != null && !externalImageUrl.isBlank()) {
                try {
                    MultipartFile downloaded = extractor.downloadImageAsMultipartFile(externalImageUrl);
                    imageUrl = fileStorageService.upload(downloaded);
                    log.info("이미지 내부 저장 URL: {}", imageUrl);
                } catch (IOException e) {
                    log.warn("이미지 다운로드/업로드 실패: {}", e.getMessage());
                }
            }

            // 4. ClothesType 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
                log.info("상품명 기반 ClothesType 추정 결과: {}", type);
            }

            // 5. 속성 추출
            List<Attribute> attributes = extractor.extractAttributes(doc, name);

            // 6. DTO 변환: 추출 속성을 DB 정의 기반으로 selectable 값 매핑
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    // DB 칼럼명 후보 가져오기
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES.getOrDefault(attr.type(), List.of(attr.type().name()));

                    // DB 조회 후 selectable 값 매핑
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

            // 7. DTO 반환
            return new ClothesDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                imageUrl,
                type,
                finalAttributes
            );

        } catch (IOException e) {
            throw new ClothesExtractionException("에이블리 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }
}