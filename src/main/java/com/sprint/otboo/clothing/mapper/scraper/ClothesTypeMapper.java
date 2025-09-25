package com.sprint.otboo.clothing.mapper.scraper;

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
        if (lower.matches(".*(카디건|가디건|자켓|재킷|코트|점퍼|패딩|아우터|후드집업|트렌치|야상|무스탕|퍼|jacket|coat|cardigan|outer|parka|zip[- ]?up|mustang).*")) {
            return ClothesType.OUTER;
        }

        // ===== 원피스 DRESS =====
        if (lower.matches(".*(원피스|드레스|맥시드레스|미니드레스|셔링원피스|랩원피스|투피스|dress|onepiece|twopiece).*")) {
            return ClothesType.DRESS;
        }

        // ===== 상의 TOP =====
        if (lower.matches(".*(셔츠|티셔츠|후드|맨투맨|니트|블라우스|탑|나시|탱크탑|크롭탑|크롭나시|스웨터|스웨트|집업|폴로|polo|henley|hoodie|sweater|blouse|tanktop|croptop|top).*")) {
            return ClothesType.TOP;
        }

        // ===== 하의 BOTTOM =====
        if (lower.matches(".*(바지|팬츠|청바지|슬랙스|레깅스|치마|스커트|조거팬츠|반바지|숏팬츠|쇼츠|트레이닝팬츠|스웻팬츠|pants|jeans|skirt|shorts|leggings|jogger|sweatpants).*")) {
            return ClothesType.BOTTOM;
        }

        // ===== 속옷 UNDERWEAR =====
        if (lower.matches(".*(속옷|언더웨어|브라|팬티|란제리|슬립|캐미솔|서포트웨어|underwear|bra|panty|lingerie|slip|camisole).*")) {
            return ClothesType.UNDERWEAR;
        }

        // ===== 신발 SHOES =====
        if (lower.matches(".*(신발|슈즈|운동화|런닝화|워킹화|러닝화|트레이너|하이탑|로우탑|슬립온|구두|로퍼|옥스포드|더비|모카신|브로그|부츠|앵클부츠|첼시부츠|워커부츠|레인부츠|샌들|슬리퍼|플랫|발레리나|뮬|하이힐|펌프스|웨지힐|crocs|sneakers|running shoes|trainers|high[- ]?top|low[- ]?top|slip[- ]?on|loafer|oxford|derby|moccasin|brogue|boots|ankle boots|chelsea boots|walker boots|rain boots|sandals|slipper|flat|ballerina|mule|heels|pumps|wedge).*")) {
            return ClothesType.SHOES;
        }

        // ===== 양말 SOCKS =====
        if (lower.matches(".*(양말|삭스|스타킹|니삭스|socks|stockings).*")) {
            return ClothesType.SOCKS;
        }

        // ===== 모자 HAT =====
        if (lower.matches(".*(모자|캡|비니|햇|버킷햇|페도라|카우보이햇|볼캡|플로피햇|hat|cap|beanie|fedora|bucket|cowboy|baseball|floppy).*")) {
            return ClothesType.HAT;
        }

        // ===== 가방 BAG =====
        if (lower.matches(".*(가방|백|클러치|백팩|토트|크로스백|숄더백|힙색|파우치|캐리어|지갑|서류가방|메신저백|슬링백|버킷백|bag|backpack|clutch|tote|crossbody|shoulder|pouch|hipbag|wallet|suitcase|briefcase|messenger|sling|bucket).*")) {
            return ClothesType.BAG;
        }

        // ===== 악세서리 ACCESSORY =====
        if (lower.matches(".*(악세서리|액세서리|목걸이|반지|팔찌|귀걸이|시계|벨트|헤어밴드|핀|선글라스|브로치|타이|키링|피어싱|brooch|accessory|necklace|ring|bracelet|earring|watch|belt|hairband|pin|sunglasses|keyring|tie).*")) {
            return ClothesType.ACCESSORY;
        }

        // ===== 스카프 SCARF =====
        if (lower.matches(".*(스카프|머플러|넥워머|목도리|머리스카프|반다나|scarf|stole|muffler|neckwarmer|bandana).*")) {
            return ClothesType.SCARF;
        }

        // ===== 기타 ETC =====
        return ClothesType.ETC;
    }
}