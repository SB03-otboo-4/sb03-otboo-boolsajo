package com.sprint.otboo.weather.integration.kakao;

import com.sprint.otboo.weather.integration.kakao.client.KakaoLocalClient;
import com.sprint.otboo.weather.integration.kakao.dto.KakaoCoord2RegioncodeResponse;
import com.sprint.otboo.weather.service.LocationNameResolver;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoLocationNameResolver implements LocationNameResolver {

    private final KakaoLocalClient client;

    @Override
    public List<String> resolve(double longitude, double latitude) {
        KakaoCoord2RegioncodeResponse body = client.coord2RegionCode(longitude, latitude);
        if (body == null || body.documents() == null || body.documents().isEmpty()) {
            return List.of();
        }
        var chosen = body.documents().stream()
            .filter(d -> "B".equalsIgnoreCase(d.region_type()))
            .findFirst()
            .or(() -> body.documents().stream().findFirst());

        if (chosen.isEmpty()) return List.of();

        var d = chosen.get();
        List<String> names = new ArrayList<>();
        addIfPresent(names, d.region_1depth_name());
        addIfPresent(names, d.region_2depth_name());
        addIfPresent(names, d.region_3depth_name());
        return names;
    }

    private static void addIfPresent(List<String> acc, String s) {
        if (s != null && !s.isBlank()) acc.add(s);
    }
}
