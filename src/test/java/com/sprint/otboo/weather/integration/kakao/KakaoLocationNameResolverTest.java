package com.sprint.otboo.weather.integration.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.integration.kakao.client.KakaoLocalClient;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse.Document;
import com.sprint.otboo.weather.service.LocationNameResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("KakaoLocationNameResolver 테스트")
class KakaoLocationNameResolverTest {

    @Test
    void 법정동과_행정동이_둘_다_오면_법정동을_우선한다() {
        KakaoLocalClient client = Mockito.mock(KakaoLocalClient.class);
        LocationNameResolver resolver = new KakaoLocationNameResolver(client);

        var b = new Document("B","서울특별시 중구 태평로1가","서울특별시","중구","태평로1가","", "111111", 0, 0);
        var h = new Document("H","서울특별시 중구 태평로1가","서울특별시","중구","태평로1가","", "222222", 0, 0);
        var res = new KakaoCoord2RegioncodeResponse(new KakaoCoord2RegioncodeResponse.Meta(2), List.of(b, h));

        Mockito.when(client.coord2RegionCode(126.9780, 37.5665)).thenReturn(res);

        List<String> names = resolver.resolve(37.5665, 126.9780);
        assertThat(names).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 결과가_비면_빈_리스트를_반환한다() {
        KakaoLocalClient client = Mockito.mock(KakaoLocalClient.class);
        LocationNameResolver resolver = new KakaoLocationNameResolver(client);
        var res = new KakaoCoord2RegioncodeResponse(new KakaoCoord2RegioncodeResponse.Meta(0), List.of());

        Mockito.when(client.coord2RegionCode(126.9780, 37.5665)).thenReturn(res);

        assertThat(resolver.resolve(37.5665, 126.9780)).isEmpty();
    }

    @Test
    void 공란_필드는_제외하고_반환한다() {
        KakaoLocalClient client = Mockito.mock(KakaoLocalClient.class);
        LocationNameResolver resolver = new KakaoLocationNameResolver(client);

        var b = new Document("B","경기도 성남시 분당구","","분당구","삼평동","", "4113510900",0,0);
        var res = new KakaoCoord2RegioncodeResponse(new KakaoCoord2RegioncodeResponse.Meta(1), List.of(b));

        Mockito.when(client.coord2RegionCode(127.1086, 37.4012)).thenReturn(res);

        List<String> names = resolver.resolve(37.4012, 127.1086);
        assertThat(names).containsExactly("분당구","삼평동");
    }
}
