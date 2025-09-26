package com.sprint.otboo.clothing.mapper.scraper;

import com.sprint.otboo.clothing.entity.attribute.Size;
import java.util.List;
import java.util.Locale;

/**
 * 사이즈 문자열 매핑 유틸리티
 *
 * <p>임의의 문자열을 {@link Size} Enum으로 변환하는 기능을 제공</p>
 *
 * <ul>
 *   <li>다양한 영문/한글 표기를 지원</li>
 *   <li>대소문자 및 공백 무시</li>
 *   <li>포함된 키워드 기반으로 매핑</li>
 *   <li>완전 일치 키워드 우선, 이후 부분 일치로 매핑</li>
 *   <li>매칭 불가능 시 {@link Size#UNKNOWN} 반환</li>
 * </ul>
 */
public class SizeMapper {

    /**
     * 문자열을 {@link Size} Enum으로 매핑
     *
     * @param value 사이즈 문자열
     * @return {@link Size} 변환된 Enum 값, 매칭되지 않으면 {@link Size#UNKNOWN}
     */
    public static Size map(String value) {
        if (value == null || value.isBlank()) return Size.UNKNOWN;

        String v = value.toLowerCase(Locale.ROOT).trim();

        // 1. 완전 일치 먼저 확인
        for (Size size : Size.values()) {
            if (size == Size.UNKNOWN) continue;
            for (String keyword : getKeywords(size)) {
                if (v.equalsIgnoreCase(keyword)) {
                    return size;
                }
            }
        }

        // 2. 부분 일치 확인
        for (Size size : Size.values()) {
            if (size == Size.UNKNOWN) continue;
            for (String keyword : getKeywords(size)) {
                if (v.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return size;
                }
            }
        }

        return Size.UNKNOWN;
    }

    /**
     * 사이즈별 매칭 키워드 목록 반환
     *
     * <p>영문 표기, 한글 표기, 축약형 등 다양한 케이스를 포함</p>
     *
     * @param size {@link Size} Enum 값
     * @return 해당 사이즈와 매칭 가능한 문자열 목록
     */
    private static List<String> getKeywords(Size size) {
        return switch (size) {
            case FREE -> List.of("FREE", "원사이즈", "프리", "ONE SIZE", "ONESIZE", "FREE SIZE");
            case ONE_SIZE -> List.of("ONE", "ONE-SIZE", "ONE SIZE", "One", "Single Size");
            case XXXL -> List.of("XXXL", "3XL", "3X-LARGE", "트리플엑스라지", "쓰리엑스", "쓰리엑스라지");
            case XXL -> List.of("XXL", "XX-LARGE", "XX-Large", "2XL", "2X-LARGE", "더블엑스라지", "투엑스", "투엑스라지");
            case XL -> List.of("XL", "X-LARGE", "Extra Large", "EXTRA LARGE", "빅라지", "엑스라지", "엑스엘");
            case XS -> List.of("XS", "X-SMALL", "X-Small", "Extra Small", "EXTRA SMALL", "엑스스몰", "엑스 스몰", "작은사이즈");
            case L -> List.of("Large", "LARGE", "large", "Big", "BIG", "big", "라지", "빅");
            case M -> List.of("Medium", "medium", "MEDIUM", "미디움", "미디엄", "중간");
            case S -> List.of("Small","small", "SMALL", "스몰", "스몰사이즈");
            default -> List.of();
        };
    }
}