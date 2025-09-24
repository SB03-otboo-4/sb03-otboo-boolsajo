package com.sprint.otboo.clothing.mapper;

import com.sprint.otboo.clothing.entity.ClothesType;

/**
 * 사이트에서 추출한 카테고리 문자열을 ClothesType으로 변환하는 매퍼
 *
 * <p>
 * - 문자열 매칭 기반 단순 매핑
 * - 한국어/영어 의류명 모두 매핑 가능
 * - 사이트별 카테고리명은 다를 수 있으므로 확장 가능
 * </p>
 */
public class ClothesTypeMapper {

    /**
     * 카테고리 문자열을 ClothesType으로 매핑
     *
     * @param category 사이트에서 추출된 카테고리 문자열
     * @return 매핑된 ClothesType, 매칭되지 않으면 ETC
     */
    public static ClothesType mapToClothesType(String category) {
        if (category == null || category.isBlank()) {
            return ClothesType.ETC;
        }

        String lower = category.toLowerCase();

        // ===== 아우터 OUTER (우선순위 1) =====
        if (lower.matches(".*(카디건|가디건|자켓|재킷|코트|점퍼|패딩|아우터|후드집업|트렌치|야상|무스탕|퍼|jacket|coat|cardigan|outer|parka|zip[- ]?up).*")) {
            return ClothesType.OUTER;
        }

        // ===== 원피스 DRESS =====
        if (lower.matches(".*(원피스|드레스|맥시드레스|미니드레스|셔링원피스|랩원피스|투피스|dress|onepiece|twopiece).*")) {
            return ClothesType.DRESS;
        }

        // ===== 상의 TOP =====
        if (lower.matches(".*(셔츠|티셔츠|후드|맨투맨|니트|블라우스|탑|나시|탱크탑|크롭탑|크롭나시|스웨터|스웨트|집업|hoodie|sweater|blouse|tanktop|croptop|top).*")) {
            return ClothesType.TOP;
        }

        // ===== 하의 BOTTOM =====
        if (lower.matches(".*(바지|팬츠|청바지|슬랙스|레깅스|치마|스커트|조거팬츠|반바지|숏팬츠|pants|jeans|skirt|shorts|leggings).*")) {
            return ClothesType.BOTTOM;
        }

        // ===== 속옷 UNDERWEAR =====
        if (lower.matches(".*(속옷|언더웨어|브라|팬티|란제리|슬립|캐미솔|서포트웨어|underwear|bra|panty|lingerie|slip|camisole).*")) {
            return ClothesType.UNDERWEAR;
        }

        // ===== 신발 SHOES =====
        if (lower.matches(".*(신발|슈즈|운동화|구두|샌들|슬리퍼|부츠|로퍼|하이힐|크록스|shoes|sneakers|boots|loafers|heels|sandals|crocs).*")) {
            return ClothesType.SHOES;
        }

        // ===== 양말 SOCKS =====
        if (lower.matches(".*(양말|삭스|스타킹|니삭스|socks|stockings).*")) {
            return ClothesType.SOCKS;
        }

        // ===== 모자 HAT =====
        if (lower.matches(".*(모자|캡|비니|햇|버킷햇|페도라|hat|cap|beanie|fedora|bucket).*")) {
            return ClothesType.HAT;
        }

        // ===== 가방 BAG =====
        if (lower.matches(".*(가방|백|클러치|백팩|토트|크로스백|숄더백|힙색|파우치|캐리어|지갑|bag|backpack|clutch|tote|crossbody|shoulder|pouch|hipbag|wallet|suitcase).*")) {
            return ClothesType.BAG;
        }

        // ===== 스카프 SCARF =====
        if (lower.matches(".*(스카프|머플러|넥워머|목도리|머리스카프|scarf|stole|muffler).*")) {
            return ClothesType.SCARF;
        }

        // ===== 악세서리 ACCESSORY =====
        if (lower.matches(".*(악세서리|액세서리|목걸이|반지|팔찌|귀걸이|시계|벨트|헤어밴드|핀|선글라스|accessory|necklace|ring|bracelet|earring|watch|belt|hairband|pin|sunglasses).*")) {
            return ClothesType.ACCESSORY;
        }

        // ===== 기타 ETC =====
        return ClothesType.ETC;
    }
}