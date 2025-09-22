package com.sprint.otboo;

import com.sprint.otboo.weather.service.LocationNameResolver;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class Sb03OtbooBoolsajoApplicationTests {

	@MockitoBean
	private LocationNameResolver locationNameResolver;

	@MockitoBean
	private WeatherLocationQueryService weatherLocationQueryService;

	@Test
	void contextLoads() {
	}

}
