package com.sprint.otboo.clothing.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.mapper.scraper.ColorMapper;
import com.sprint.otboo.clothing.mapper.scraper.MaterialMapper;
import com.sprint.otboo.clothing.mapper.scraper.SeasonMapper;
import com.sprint.otboo.clothing.mapper.scraper.SizeMapper;
import com.sprint.otboo.clothing.mapper.scraper.ThicknessMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * 의상 정보를 추출하는 유틸 클래스
 *
 * <p>동작 흐름:</p>
 * <ol>
 *   <li>JSoup으로 HTML 문서 로드</li>
 *   <li>상품명, 이미지, 카테고리 추출</li>
 *   <li>카테고리/상품명 기반 ClothesType 결정</li>
 *   <li>{@link ClothesAttributeExtractor}를 통해 속성 추출</li>
 *   <li>추출 속성을 DB 정의 기반 selectable 값으로 보정</li>
 *   <li>{@link ClothesDto} 반환</li>
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

        // JSON-LD, HTML, 텍스트 기반 속성 추출
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
            if (offers.isArray()) offers.forEach(offer -> addJsonLdAttr(attributes, existing, AttributeType.COLOR, offer.get("color")));
            else addJsonLdAttr(attributes, existing, AttributeType.COLOR, offers.get("color"));
        }

        // description 텍스트 기반 속성 추출
        if (node.has("description")) {
            String description = node.get("description").asText();
            log.info("JSON-LD description 기반 속성 추출 시도: {}", description);
            extractFromText(description, attributes, existing);
        }
    }

    // JSON-LD 속성 추가
    private void addJsonLdAttr(List<Attribute> attributes, Set<String> existing, AttributeType type, JsonNode node) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(val -> {
                addIfValid(attributes, existing, type, val.asText());
                log.info("JSON-LD 배열 속성 추출: {} -> {}", type, val.asText());
            });
        } else {
            addIfValid(attributes, existing, type, node.asText());
            log.info("JSON-LD 단일 속성 추출: {} -> {}", type, node.asText());
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
            addIfValid(target, existing, type, el.text());
            log.info("HTML 속성 추출: {} -> {}", type, el.text());
        }
    }

    // 텍스트에서 속성 단어 매핑 후 추출
    private void extractFromText(String text, List<Attribute> attributes, Set<String> existing) {
        for (String word : text.split("[,/ \\[\\]\\-]+")) {
            if (word.isBlank()) continue;

            // 각 Mapper를 통해 Enum 변환 후 속성 추가
            addIfValidWithLog(attributes, existing, AttributeType.COLOR, ColorMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.SIZE, SizeMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.MATERIAL, MaterialMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.SEASON, SeasonMapper.map(word));
            addIfValidWithLog(attributes, existing, AttributeType.THICKNESS, ThicknessMapper.map(word));
        }
    }

    // 유효한 Enum 타입의 속성( Color, Size, Material ) 추가
    private void addIfValidWithLog(List<Attribute> attributes, Set<String> existing, AttributeType type, Enum<?> value) {
        if (value != null && !"UNKNOWN".equals(value.name()) && existing.add(value.name())) {
            attributes.add(new Attribute(type, value.name()));
            log.info("Text 기반 매핑: {} -> {}", type, value.name());
        }
    }

    // 유효한 문자열 속성 추가
    private void addIfValid(List<Attribute> attributes, Set<String> existing, AttributeType type, String value) {
        if (value != null && !value.isBlank() && existing.add(value)) {
            attributes.add(new Attribute(type, value));
        }
    }
}