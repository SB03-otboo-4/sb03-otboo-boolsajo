package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.integration.owm.WeatherOwmProperties;
import com.sprint.otboo.weather.integration.owm.mapper.WindStrengthResolver;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("WeatherServiceImpl 유틸 및 분기 테스트")
class WeatherServiceImplTest {

    @Test
    void x_y_로_위치가_조회되면_그대로_반환된다() throws Exception {
        Object service = instantiateService();
        WeatherLocationRepository locationRepo =
            (WeatherLocationRepository) injected(service, WeatherLocationRepository.class);

        Class<?> locationClass = Class.forName("com.sprint.otboo.weather.entity.WeatherLocation");
        Object found = Mockito.mock(locationClass);

        // findFirstByXAndY 리턴타입에 맞춰 동적으로 값 세팅
        Method byXY = WeatherLocationRepository.class.getMethod("findFirstByXAndY", int.class, int.class);
        Object hitReturn = wrapHit(byXY.getReturnType(), found);
        Mockito.doReturn(hitReturn).when(locationRepo).findFirstByXAndY(60, 127);

        WeatherLocationResponse dto =
            new WeatherLocationResponse(37.5, 127.0, 60, 127, List.of("서울"));

        Method resolve = declared(service.getClass(), "resolveLocationEntity", WeatherLocationResponse.class);
        Object result = resolve.invoke(service, dto);

        Assertions.assertThat(result).isSameAs(found);
    }

    @Test
    void x_y_조회_실패시_위경도로_조회되면_반환된다() throws Exception {
        Object service = instantiateService();
        WeatherLocationRepository locationRepo =
            (WeatherLocationRepository) injected(service, WeatherLocationRepository.class);

        Class<?> locationClass = Class.forName("com.sprint.otboo.weather.entity.WeatherLocation");
        Object found = Mockito.mock(locationClass);

        // x,y MISS
        Method byXY = WeatherLocationRepository.class.getMethod("findFirstByXAndY", int.class, int.class);
        Object missReturn = wrapMiss(byXY.getReturnType());
        Mockito.doReturn(missReturn).when(locationRepo).findFirstByXAndY(60, 127);

        // lat/lon HIT
        Method byLatLon = WeatherLocationRepository.class.getMethod(
            "findFirstByLatitudeAndLongitude", BigDecimal.class, BigDecimal.class);
        Object hitReturn = wrapHit(byLatLon.getReturnType(), found);
        Mockito.doReturn(hitReturn).when(locationRepo)
            .findFirstByLatitudeAndLongitude(new BigDecimal("37.5"), new BigDecimal("127.0"));

        WeatherLocationResponse dto =
            new WeatherLocationResponse(37.5, 127.0, 60, 127, List.of("서울"));

        Method resolve = declared(service.getClass(), "resolveLocationEntity", WeatherLocationResponse.class);
        Object result = resolve.invoke(service, dto);

        Assertions.assertThat(result).isSameAs(found);
    }

    @Test
    void x_y와_위경도_모두_조회_실패시_예외가_발생한다() throws Exception {
        Object service = instantiateService();
        WeatherLocationRepository locationRepo =
            (WeatherLocationRepository) injected(service, WeatherLocationRepository.class);

        // x,y MISS
        Method byXY = WeatherLocationRepository.class.getMethod("findFirstByXAndY", int.class, int.class);
        Object missXY = wrapMiss(byXY.getReturnType());
        Mockito.doReturn(missXY).when(locationRepo).findFirstByXAndY(0, 0);

        // lat/lon MISS
        Method byLatLon = WeatherLocationRepository.class.getMethod(
            "findFirstByLatitudeAndLongitude", BigDecimal.class, BigDecimal.class);
        Object missLL = wrapMiss(byLatLon.getReturnType());
        Mockito.doReturn(missLL).when(locationRepo)
            .findFirstByLatitudeAndLongitude(new BigDecimal("0.0"), new BigDecimal("0.0"));

        WeatherLocationResponse dto =
            new WeatherLocationResponse(0.0, 0.0, 0, 0, List.of("N/A"));

        Method resolve = declared(service.getClass(), "resolveLocationEntity", WeatherLocationResponse.class);

        Assertions.assertThatThrownBy(() -> {
            try {
                resolve.invoke(service, dto);
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
                throw e;
            }
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    void key_메서드는_null과_값이_있는_경우를_구분한다() throws Exception {
        Class<?> impl = Class.forName("com.sprint.otboo.weather.service.WeatherServiceImpl");
        Method key = declared(impl, "key", Instant.class, Instant.class);

        String k1 = (String) key.invoke(null, Instant.parse("2025-01-01T00:00:00Z"), null);
        String k2 = (String) key.invoke(null,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-02T03:00:00Z"));

        Assertions.assertThat(k1).isEqualTo("2025-01-01T00:00:00Z|");
        Assertions.assertThat(k2).isEqualTo("2025-01-01T00:00:00Z|2025-01-02T03:00:00Z");
    }

    // ===== helpers =====

    private static Object wrapHit(Class<?> returnType, Object element) {
        if (Optional.class.isAssignableFrom(returnType)) {
            return Optional.of(element);
        } else if (List.class.isAssignableFrom(returnType)) {
            return List.of(element);
        } else if (Stream.class.isAssignableFrom(returnType)) {
            return Stream.of(element);
        } else {
            // 단일 엔티티 반환 등
            return element;
        }
    }

    private static Object wrapMiss(Class<?> returnType) {
        if (Optional.class.isAssignableFrom(returnType)) {
            return Optional.empty();
        } else if (List.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        } else if (Stream.class.isAssignableFrom(returnType)) {
            return Stream.empty();
        } else {
            // 단일 엔티티 반환이면 null (MISS)
            return null;
        }
    }

    private static Object instantiateService() throws Exception {
        Class<?> clazz = Class.forName("com.sprint.otboo.weather.service.WeatherServiceImpl");
        java.lang.reflect.Constructor<?> ctor =
            Arrays.stream(clazz.getDeclaredConstructors())
                .sorted((a, b) -> b.getParameterCount() - a.getParameterCount())
                .findFirst()
                .orElseThrow();
        ctor.setAccessible(true);

        Map<Class<?>, Object> mocks = new HashMap<>();
        mocks.put(WeatherLocationRepository.class, Mockito.mock(WeatherLocationRepository.class));
        mocks.put(WeatherRepository.class, Mockito.mock(WeatherRepository.class));
        mocks.put(WeatherDataClient.class, Mockito.mock(WeatherDataClient.class));
        mocks.put(WeatherMapper.class, Mockito.mock(WeatherMapper.class));
        mocks.put(WeatherOwmProperties.class, Mockito.mock(WeatherOwmProperties.class));
        mocks.put(WindStrengthResolver.class, Mockito.mock(WindStrengthResolver.class));

        Class<?>[] pts = ctor.getParameterTypes();
        Object[] args = new Object[pts.length];
        for (int i = 0; i < pts.length; i++) {
            Class<?> pt = pts[i];
            Object dep = null;
            for (Map.Entry<Class<?>, Object> e : mocks.entrySet()) {
                if (pt.isAssignableFrom(e.getKey())) {
                    dep = e.getValue();
                    break;
                }
            }
            if (dep == null) dep = Mockito.mock(pt);
            args[i] = dep;
        }

        Object service = ctor.newInstance(args);
        ServiceHolder.map.put(service, mocks);
        return service;
    }

    private static Object injected(Object service, Class<?> type) {
        Map<Class<?>, Object> m = ServiceHolder.map.get(service);
        if (m == null) throw new IllegalStateException("no mocks");
        for (Map.Entry<Class<?>, Object> e : m.entrySet()) {
            if (type.isAssignableFrom(e.getKey())) return e.getValue();
        }
        throw new IllegalStateException("mock not found: " + type.getName());
    }

    private static Method declared(Class<?> t, String name, Class<?>... p) throws Exception {
        Method m = t.getDeclaredMethod(name, p);
        m.setAccessible(true);
        return m;
    }

    private static final class ServiceHolder {
        static final Map<Object, Map<Class<?>, Object>> map = new IdentityHashMap<>();
    }
}
