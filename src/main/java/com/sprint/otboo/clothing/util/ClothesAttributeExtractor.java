package com.sprint.otboo.clothing.util;

import static com.sprint.otboo.clothing.mapper.scraper.SizeMapper.PRIORITY_ORDER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.entity.attribute.Size;
import com.sprint.otboo.clothing.mapper.scraper.ColorMapper;
import com.sprint.otboo.clothing.mapper.scraper.MaterialMapper;
import com.sprint.otboo.clothing.mapper.scraper.SeasonMapper;
import com.sprint.otboo.clothing.mapper.scraper.SizeMapper;
import com.sprint.otboo.clothing.mapper.scraper.ThicknessMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 속성을 HTML 문서, JSON-LD, 텍스트에서 추출하는 유틸 클래스.
 *
 * <p>동작 흐름:</p>
 * <ol>
 *   <li>JSoup으로 HTML 문서 로드</li>
 *   <li>상품명, 설명, JSON-LD, HTML 요소에서 속성 추출</li>
 *   <li>AttributeType별 Enum으로 매핑</li>
 *   <li>DB 정의 기반 selectable 값으로 보정</li>
 *   <li>{@link ClothesDto} 생성 시 사용 가능한 속성 반환</li>
 * </ol>
 *
 * <p>추출 속성:</p>
 * <ul>
 *   <li>{@link AttributeType#COLOR} 색상</li>
 *   <li>{@link AttributeType#SIZE} 사이즈</li>
 *   <li>{@link AttributeType#MATERIAL} 소재</li>
 *   <li>{@link AttributeType#SEASON} 계절</li>
 *   <li>{@link AttributeType#THICKNESS} 두께</li>
 * </ul>
 */
@Slf4j
@Component
public class ClothesAttributeExtractor {

    // 내부 속성 표현용 레코드
    public record Attribute(AttributeType type, String value) {}

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AttributeType -> DB 컬럼명 매핑
     *
     * <ul>
     *   <li>한글/영어 대소문자 변형 포함</li>
     *   <li>DB 조회 또는 매핑용 참조 테이블</li>
     * </ul>
     */
    public static final Map<AttributeType, List<String>> TYPE_TO_DB_NAMES = Map.of(
        AttributeType.COLOR, Arrays.asList("색상", "Color", "COLOR", "color"),
        AttributeType.SIZE, Arrays.asList("사이즈", "Size", "SIZE", "size"),
        AttributeType.MATERIAL, Arrays.asList("소재", "Material", "MATERIAL", "material"),
        AttributeType.SEASON, Arrays.asList("계절", "Season", "SEASON", "season"),
        AttributeType.THICKNESS, Arrays.asList("두께", "Thickness", "THICKNESS", "thickness")
    );

    /**
     * HTML 문서와 상품명 / 설명 텍스트를 기반으로 속성 추출
     *
     * @param doc HTML 문서
     * @param text 상품명 또는 설명 텍스트
     * @return 추출된 속성 리스트
     */
    public List<Attribute> extractAttributes(Document doc, String text) {
        List<Attribute> attributes = new ArrayList<>();
        Set<String> existing = new HashSet<>();

        extractFromJsonLd(doc, attributes, existing);
        extractFromHtml(doc, attributes, existing);
        extractFromText(text, attributes, existing);

        return attributes;
    }

    // =================== JSON-LD / HTML / Text 추출 ===================

    // HTML 문서 내 JSON-LD 스크립트에서 속성 추출
    private void extractFromJsonLd(Document doc, List<Attribute> attributes, Set<String> existing) {
        doc.select("script[type=application/ld+json]").forEach(script -> {
            try {
                JsonNode root = objectMapper.readTree(script.html());
                if (root.isArray()) {
                    for (JsonNode node : root) parseJsonLdNode(node, attributes, existing);
                } else {
                    parseJsonLdNode(root, attributes, existing);
                }
            } catch (Exception ignored) {
                log.warn("JSON-LD 파싱 실패, 무시: {}", ignored.getMessage());
            }
        });
    }

    // JSON-LD Node 단위로 속성 파싱
    private void parseJsonLdNode(JsonNode node, List<Attribute> attributes, Set<String> existing) {
        if (!"Product".equals(node.path("@type").asText())) return;

        // 주요 속성 추출
        addJsonLdAttr(attributes, existing, AttributeType.COLOR, node.get("color"));
        addJsonLdAttr(attributes, existing, AttributeType.SIZE, node.get("size"));
        addJsonLdAttr(attributes, existing, AttributeType.MATERIAL, node.get("material"));
        addJsonLdAttr(attributes, existing, AttributeType.SEASON, node.get("season"));
        addJsonLdAttr(attributes, existing, AttributeType.THICKNESS, node.get("thickness"));

        // offers 속성 내 color도 추출
        JsonNode offers = node.get("offers");
        if (offers != null) {
            if (offers.isArray()) offers.forEach(offer -> extractOfferAttributes(offer, attributes, existing));
            else extractOfferAttributes(offers, attributes, existing);
        }

        // description 텍스트 기반 속성 추출
        if (node.has("description")) extractFromText(node.get("description").asText(), attributes, existing);
    }

    // AttributeType별 문자열을 Enum으로 매핑
    private Enum<?> mapNodeValue(AttributeType type, String value) {
        if (value == null || value.isBlank()) return null;
        return switch (type) {
            case COLOR -> ColorMapper.map(value);
            case SIZE -> SizeMapper.map(value);
            case MATERIAL -> MaterialMapper.map(value);
            case SEASON -> SeasonMapper.map(value);
            case THICKNESS -> ThicknessMapper.map(value);
        };
    }

    // offers 속성에서 컬러 / 사이즈 / 소재 추출
    private void extractOfferAttributes(JsonNode offer, List<Attribute> attributes, Set<String> existing) {
        String colorVal = offer.path("color").asText(null);
        if (colorVal != null) {
            addIfValidWithLog(attributes, existing, AttributeType.COLOR, ColorMapper.map(colorVal));
            addIfValidWithLog(attributes, existing, AttributeType.SIZE, SizeMapper.map(colorVal));
            addIfValidWithLog(attributes, existing, AttributeType.MATERIAL, MaterialMapper.map(colorVal));
        }

        String sizeVal = offer.path("size").asText(null);
        if (sizeVal != null) addIfValidWithLog(attributes, existing, AttributeType.SIZE, SizeMapper.map(sizeVal));

        String materialVal = offer.path("material").asText(null);
        if (materialVal != null) addIfValidWithLog(attributes, existing, AttributeType.MATERIAL, MaterialMapper.map(materialVal));
    }

    // JSON-LD 속성 추가
    private void addJsonLdAttr(List<Attribute> attributes, Set<String> existing, AttributeType type, JsonNode node) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(val -> {
                addIfValidWithLog(attributes, existing, type, mapNodeValue(type, val.asText()));
            });
        } else {
            addIfValidWithLog(attributes, existing, type, mapNodeValue(type, node.asText()));
        }
    }

    // HTML 문서 내 요소에서 속성 추출
    private void extractFromHtml(Document doc, List<Attribute> attributes, Set<String> existing) {
        safeAdd(attributes, existing, AttributeType.COLOR, doc.select("ul.product-info-color li button"));
        safeAdd(attributes, existing, AttributeType.SIZE, doc.select("ul.product-info-size li button"));
        safeAdd(attributes, existing, AttributeType.MATERIAL, doc.select("ul.product-info-material li span"));
        safeAdd(attributes, existing, AttributeType.SEASON, doc.select("ul.product-info-season li span"));
        safeAdd(attributes, existing, AttributeType.THICKNESS, doc.select("ul.product-info-thickness li span"));
    }

    // HTML 요소에서 안전하게 속성 추가
    private void safeAdd(List<Attribute> target, Set<String> existing, AttributeType type, Elements elems) {
        for (Element el : elems) {
            addIfValidWithLog(target, existing, type, mapNodeValue(type, el.text()));
        }
    }

    // 텍스트에서 속성 단어 매핑 후 추출
    private void extractFromText(String text, List<Attribute> attributes, Set<String> existing) {
        if (text == null || text.isBlank()) return;

        // 문장 전체를 MaterialMapper에 넘겨서 혼방/복합 키워드 우선 처리
        addIfValidWithLog(attributes, existing, AttributeType.MATERIAL, MaterialMapper.map(text));

        // 단어 단위로 나누어 Color, Size, Season, Thickness 매핑
        for (String word : text.split("[,/ \\[\\]\\-]+")) {
            if (word.isBlank()) continue;

            addIfValidWithLog(attributes, existing, AttributeType.COLOR, ColorMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.SIZE, SizeMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.SEASON, SeasonMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.THICKNESS, ThicknessMapper.map(word));
        }
    }

    // 유효한 Enum 타입의 속성( Color, Size, Material ) 추가
    private void addIfValidWithLog(List<Attribute> attributes, Set<String> existing, AttributeType type, Enum<?> value) {
        if (value == null || "UNKNOWN".equals(value.name())) return;

        // SIZE 타입은 우선순위 비교 후 기존 값보다 크면 교체
        if (type == AttributeType.SIZE) {
            Size newSize = (Size) value;
            Optional<Attribute> existingSizeAttr = attributes.stream()
                .filter(attr -> attr.type == AttributeType.SIZE)
                .findFirst();

            if (existingSizeAttr.isEmpty()) {
                attributes.add(new Attribute(type, newSize.name()));
                log.info("Mapped Attribute (SIZE, first): {}", newSize.name());
            } else {
                Size existingSize = Size.valueOf(existingSizeAttr.get().value());

                // PRIORITY_ORDER 순서 기준으로 큰 값 유지
                if (PRIORITY_ORDER.indexOf(newSize) < PRIORITY_ORDER.indexOf(existingSize)) {
                    attributes.remove(existingSizeAttr.get());
                    attributes.add(new Attribute(type, newSize.name()));
                    log.info("Mapped Attribute (SIZE, replaced by bigger): {}", newSize.name());
                }
            }
            return;
        }

        String key = type + ":" + value.name();
        if (!existing.add(key)) return;

        String formatted = formatEnumValue(value.name());
        attributes.add(new Attribute(type, formatted));
        log.info("Mapped Attribute: {} -> {}", type, formatted);
    }

    // Enum 이름을 PascalCase 형식으로 변환
    private String formatEnumValue(String name) {
        if (name == null || name.isBlank()) return name;
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    // 선택값 보정
    public String matchSelectableValue(String value, String selectableValues) {
        if (value == null || value.isBlank()) return value;

        // selectableValues가 null/빈 문자열이면 빈 리스트로 처리
        List<String> values = selectableValues == null || selectableValues.isBlank()
            ? Collections.emptyList()
            : Arrays.asList(selectableValues.split(","));

        // 리스트 내에서 value와 대소문자 구분 없이 일치하는 값 반환
        return values.stream()
            .filter(sel -> sel.equalsIgnoreCase(value))
            .findFirst()
            .orElseGet(() -> {
                log.warn("SelectableValues에 없는 값, 그대로 사용: {}", value);
                return value;
            });
    }

    // 마지막 Breadcrub( 카테고리: ex. 의류 > 상의 > 후드 집업 ) 반환, 없으면 기본값( ETC )
    public String getLastBreadcrumbOrDefault(Document doc, String cssQuery, String defaultValue) {
        Elements crumbs = doc.select(cssQuery);
        return crumbs.isEmpty() ? defaultValue : crumbs.last().text();
    }

    // CSS 선택자 기반 속성 값 반환, 없으면 기본 값( ETC )
    public String getAttrOrDefault(Document doc, String cssQuery, String attr, String defaultValue) {
        Element el = doc.selectFirst(cssQuery);
        return el != null ? el.attr(attr) : defaultValue;
    }

    // 외부 이미지 URL 다운로드 후 MultipartFile로 변환
    public MultipartFile downloadImageAsMultipartFile(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);

        // URL 경로에서 확장자 추출
        String extension = "";
        String path = url.getPath();
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1) extension = path.substring(lastDot);

        // 안전한 랜덤 파일명 생성
        String finalFilename = UUID.randomUUID() + extension;
        try (InputStream in = url.openStream()) {
            byte[] bytes = in.readAllBytes();

            // 익명 MultipartFile 구현체 반환
            return new MultipartFile() {
                @Override public String getName() { return finalFilename; }
                @Override public String getOriginalFilename() { return finalFilename; }
                @Override public String getContentType() { return "application/octet-stream"; }
                @Override public boolean isEmpty() { return bytes.length == 0; }
                @Override public long getSize() { return bytes.length; }
                @Override public byte[] getBytes() { return bytes; }
                @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

                // 로컬 파일로 저장
                @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
            };
        }
    }
}