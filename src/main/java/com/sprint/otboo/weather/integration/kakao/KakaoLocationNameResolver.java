package com.sprint.otboo.weather.integration.kakao;

import com.sprint.otboo.weather.integration.kakao.client.KakaoLocalClient;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import com.sprint.otboo.weather.service.LocationNameResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoLocationNameResolver implements LocationNameResolver {

    private final KakaoLocalClient client;

    @Override
    @Cacheable(cacheNames = "kakao:region", key = "T(java.lang.String).format('%.6f:%.6f', #latitude, #longitude)")
    public List<String> resolve(double latitude, double longitude) {
        KakaoCoord2RegioncodeResponse res =
            client.coord2RegionCode(longitude, latitude);

        if (res == null || res.documents() == null || res.documents().isEmpty()) {
            return List.of();
        }

        KakaoCoord2RegioncodeResponse.Document doc =
            res.documents().stream().min(Comparator.comparing(
                    (KakaoCoord2RegioncodeResponse.Document d) ->
                        "B".equals(d.region_type()) ? 0 : 1))
                .get();

        List<String> names = new ArrayList<>();
        if (doc.region_1depth_name() != null && !doc.region_1depth_name().isBlank()) {
            names.add(doc.region_1depth_name());
        }
        if (doc.region_2depth_name() != null && !doc.region_2depth_name().isBlank()) {
            names.add(doc.region_2depth_name());
        }
        if (doc.region_3depth_name() != null && !doc.region_3depth_name().isBlank()) {
            names.add(doc.region_3depth_name());
        }
        if (doc.region_4depth_name() != null && !doc.region_4depth_name().isBlank()) {
            names.add(doc.region_4depth_name());
        }

        return List.copyOf(names);
    }
}
