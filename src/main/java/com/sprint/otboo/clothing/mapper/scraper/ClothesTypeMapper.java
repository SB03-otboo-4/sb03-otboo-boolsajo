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

        // ===== 신발 SHOES =====
        if (lower.matches(".*(클로그|에스파드리유|방한화|털신|안전화|작업화|등산화|트레킹화|하이킹화|축구화|풋살화|농구화|배구화|테니스화|골프화|실내화|고무신|반스|신발|슈즈|운동화|런닝화|워킹화|러닝화|트레이너|하이[- ]?탑|로우[- ]?탑|슬립온|구두|로퍼|옥스포드|더비|모카신|브로그|부츠|샌들|슬리퍼|플랫|발레리나|뮬|하이힐|펌프스|웨지힐|크록스|스니커즈|힐|워커|캔버스|쪼리|블로퍼|crocs|sneakers|trainers|high[- ]?top|low[- ]?top|slip[- ]?on|oxford|derby|moccasin|brogue|boots|sandal(s)?|slipper|flat|ballerina|mule|heels|pumps|wedge|vans|shoe(s)?|footwear|flip[- ]?flops|bloafers|loafer(s)?|clogs|espadrilles|galoshes).*")) {
            return ClothesType.SHOES;
        }

        // ===== 하의 BOTTOM =====
        if (lower.matches(".*(바지|팬츠|청바지|슬랙스|레깅스|치마|스커트|쇼츠|카고|pants|jeans|skirt|shorts|leggings|jogger|slacks|culottes|\bbottom\b|bottoms|cargo).*")) {
            return ClothesType.BOTTOM;
        }

        // ===== 원피스 DRESS =====
        if (lower.matches(".*(원피스|드레스|투피스|오버롤|dress|one[- ]?piece|two[- ]?piece|overall).*")) {
            return ClothesType.DRESS;
        }

        // ===== 모자 HAT =====
        if (lower.matches(".*(트릴비|바이저|베레모|머리띠|헤어핀|헤어클립|스크런치|모자|캡|비니|햇|버킷햇|페도라|카우보이햇|볼캡|플로피햇|hat|cap|beanie|fedora|bucket|cowboy|baseball|floppy|sunhat|beret|hair[- ]?clip|head[- ]?band|hair[- ]?pin|scrunchie|headwear|trilby|visors).*")) {
            return ClothesType.HAT;
        }

        // ===== 양말 SOCKS =====
        if (lower.matches(".*(양말|삭스|스타킹|니삭스|니하이|sock(s)?|stocking(s)?|knee[- ]?high|thigh[- ]high|leg[- ]?warmer(s)?).*")) {
            return ClothesType.SOCKS;
        }

        // ===== 속옷 UNDERWEAR =====
        if (lower.matches(".*(속옷|언더웨어|브라|팬티|란제리|슬립|캐미솔|서포트웨어|underwear|bra|panty|lingerie|slip|camisole|brief(s)?|boxer|shapewear|undershirt|bralette).*")) {
            return ClothesType.UNDERWEAR;
        }

        // ===== 아우터 OUTER =====
        if (lower.matches(".*(정장[- ]?마이|\b마의\b|블루종|구스다운|케이프|판초|우비|윈드[- ]?브레이커|저지|플리스|바람막이|카디건|가디건|자켓|재킷|코트|점퍼|패딩|아우터|집[- ]?업|트렌치|야상|무스탕|퍼|베스트|조끼|블레이저|jacket|coat|cardigan|outer|parka|zip[- ]?up|mustang|vest|wind[- ]?breaker|jersey|fleece|trench|fur|blouson|bomber|padding|cape|poncho|blazer).*")) {
            return ClothesType.OUTER;
        }

        // ===== 상의 TOP =====
        if (lower.matches(".*(오프[- ]?숄더|민소매|셔츠|티셔츠|후드|맨투맨|니트|블라우스|탑|나시|탱크탑|크롭탑|크롭나시|스웨터|스웨트|폴로|후디|sleeve|hoodie|hood|sweater|blouse|tank[- ]?top|crop[- ]?top|\btop\b|henley|polo|shirt|t[- ]?shirt|sweat[- ]?shirt|pullover|sleeveless[- ]?top|layering[- ]?top|sports[- ]?top|gym[- ]?top|bralette[- ]?top|long[- ]?top|tunic[- ]?top|high[- ]?neck[- ]?top|off[- ]?shoulder).*")) {
            return ClothesType.TOP;
        }

        // ===== 가방 BAG =====
        if (lower.matches(".*(가방|백|클러치|백팩|토트|힙색|파우치|캐리어|지갑|bag|backpack|clutch|tote|crossbody|pouch|hipbag|wallet|suitcase|briefcase|messenger|sling|bucket|fanny[- ]?pack|duffle).*")) {
            return ClothesType.BAG;
        }

        // ===== 스카프 SCARF =====
        if (lower.matches(".*(스카프|머플러|넥워머|목도리|반다나|scarf|stole|muffler|neckwarmer|bandana|shawl|snood|kerchief).*")) {
            return ClothesType.SCARF;
        }

        // ===== 악세서리 ACCESSORY =====
        if (lower.matches(".*(초커|팬던트|체인|참|숄|장갑|커플링|열쇠고리|아이웨어|안경|악세서리|액세서리|목걸이|반지|팔찌|발찌|귀걸이|시계|벨트|핀|선글라스|브로치|타이|키링|피어싱|eyewear|glasses|anklet|piercing|brooch|accessory|necklace|ring|bracelet|earring|watch|belt|sunglasses|keyring|tie|cufflink|choker|pendant|chain|charm|shawl|glove(s)?|mittens).*")) {
            return ClothesType.ACCESSORY;
        }

        // ===== 기타 ETC =====
        return ClothesType.ETC;
    }
}