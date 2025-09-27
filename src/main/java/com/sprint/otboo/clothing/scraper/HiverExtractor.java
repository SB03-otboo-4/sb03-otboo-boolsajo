package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.mapper.scraper.ClothesTypeMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor.Attribute;
import com.sprint.otboo.common.storage.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 하이버(hiver.co.kr) 사이트에서 의상 정보를 추출하는 {@link ClothesExtractor} 구현체.
 *
 * <p>주요 기능:
 * <ul>
 *   <li>URL 지원 여부 판단 (supports)</li>
 *   <li>상품명, 이미지, 카테고리 등 기본 정보 추출</li>
 *   <li>카테고리 기반 {@link ClothesType} 결정</li>
 *   <li>속성 추출 및 DB 기반 selectable 값 보정</li>
 *   <li>외부 이미지 다운로드 후 내부 저장소 업로드</li>
 *   <li>최종 {@link ClothesDto} 반환</li>
 * </ul>
 */
@Slf4j
@Component
public class HiverExtractor implements ClothesExtractor {

    private final ClothesAttributeExtractor extractor;
    private final ClothesAttributeDefRepository defRepository;
    private final FileStorageService fileStorageService; // 내부 저장 서비스 주입

    public HiverExtractor(
        ClothesAttributeExtractor extractor,
        ClothesAttributeDefRepository defRepository,
        // 로컬 저장 빈을 한정자로 지정, S3 전환 시 해당 빈을 한정자로 수정 적용 필요
        @Qualifier("localFileStorageService") FileStorageService fileStorageService
    ) {
        this.extractor = extractor;
        this.defRepository = defRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("hiver.co.kr");
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
            String externalImageUrl = getAttrOrDefault(doc, "meta[property=og:image]", "content", "");
            String category = getLastBreadcrumbOrDefault(doc, ".breadcrumb li a", "ETC");

            log.info("상품명 추출: {}", name);
            log.info("외부 이미지 URL 추출: {}", externalImageUrl);

            // 3. 외부 이미지 다운로드 후 내부 저장소 업로드
            String imageUrl = "";
            if (externalImageUrl != null && !externalImageUrl.isBlank()) {
                try {
                    MultipartFile downloaded = downloadImageAsMultipartFile(externalImageUrl);
                    imageUrl = fileStorageService.upload(downloaded); // 내부 URL로 변환
                    log.info("이미지 서버 저장 완료, 내부 URL: {}", imageUrl);
                } catch (IOException e) {
                    log.warn("이미지 다운로드/업로드 실패, URL 비워둠: {}", e.getMessage());
                }
            }

            // 4. ClothesType 결정
            ClothesType type = ClothesTypeMapper.mapToClothesType(category);
            if (type == ClothesType.ETC) {
                type = ClothesTypeMapper.mapToClothesType(name);
            }
            log.info("ClothesType 결정: {}", type);

            // 5. 속성 추출 및 DB 기반 selectable 값 보정
            List<Attribute> attributes = extractor.extractAttributes(doc, name);
            List<ClothesAttributeDto> finalAttributes = attributes.stream()
                .flatMap(attr -> {
                    List<String> dbNames = ClothesAttributeExtractor.TYPE_TO_DB_NAMES.getOrDefault(
                        attr.type(), List.of(attr.type().name())
                    );
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
            throw new ClothesExtractionException("하이버 URL에서 의상 정보를 추출하지 못했습니다.", e);
        }
    }

    /** 외부 이미지 URL을 다운로드하여 {@link MultipartFile}로 반환 */
    private MultipartFile downloadImageAsMultipartFile(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        String originalFilename = Paths.get(url.getPath()).getFileName().toString();
        String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\-_.]", "");
        final String finalFilename = safeFilename;

        try (InputStream in = url.openStream()) {
            byte[] bytes = in.readAllBytes();
            return new MultipartFile() {
                @Override public String getName() { return finalFilename; }
                @Override public String getOriginalFilename() { return finalFilename; }
                @Override public String getContentType() { return "application/octet-stream"; }
                @Override public boolean isEmpty() { return bytes.length == 0; }
                @Override public long getSize() { return bytes.length; }
                @Override public byte[] getBytes() { return bytes; }
                @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
                @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
            };
        }
    }

    // DB selectable 값과 매칭, 없으면 원래 값 반환
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

    // 마지막 카테고리 텍스트 추출, 없으면 기본값 반환
    private String getLastBreadcrumbOrDefault(Document doc, String cssQuery, String defaultValue) {
        Elements crumbs = doc.select(cssQuery);
        return crumbs.isEmpty() ? defaultValue : crumbs.last().text();
    }

    // CS 선택자로 속성 추출, 없으면 기본값 반환
    private String getAttrOrDefault(Document doc, String cssQuery, String attr, String defaultValue) {
        Element el = doc.selectFirst(cssQuery);
        return el != null ? el.attr(attr) : defaultValue;
    }
}