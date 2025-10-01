package com.sprint.otboo.clothing.dto.data;

import java.util.Map;

/**
 * 쇼핑몰별 상품 정보 추출 규칙을 정의하는 Record
 *
 * <p>각 쇼핑몰별로 HTML 구조가 다르므로, CSS Selector를 이용해 상품명, 이미지 URL, 카테고리 등을 추출
 * 또한 이미지 저장 여부와 HTTP 요청 시 필요한 추가 헤더를 함께 정의 가능
 *
 * <ul>
 *     <li>{@code host} : 추출 대상 쇼핑몰 도메인 (예: "musinsa.com")</li>
 *     <li>{@code nameSelector} : 상품명 추출을 위한 CSS Selector</li>
 *     <li>{@code imageSelector} : 이미지 URL 추출을 위한 CSS Selector</li>
 *     <li>{@code categorySelector} : 카테고리 추출을 위한 CSS Selector</li>
 *     <li>{@code saveImage} : 추출한 이미지를 내부 저장할지 여부</li>
 *     <li>{@code headers} : HTTP 요청 시 추가로 필요한 헤더 정보 (예: User-Agent, Referer 등)</li>
 * </ul>
 *
 * <p>예시:
 * <pre>{@code
 * MallRule rule = new MallRule(
 *     "musinsa.com",
 *     ".product-title",
 *     ".product-image > img",
 *     ".breadcrumb > li:last-child",
 *     true,
 *     Map.of("User-Agent", "Mozilla/5.0")
 * );
 * }</pre>
 */
public record MallRule(
    String host,
    String nameSelector,
    String imageSelector,
    String categorySelector,
    boolean saveImage,
    Map<String, String> headers
) {

}
