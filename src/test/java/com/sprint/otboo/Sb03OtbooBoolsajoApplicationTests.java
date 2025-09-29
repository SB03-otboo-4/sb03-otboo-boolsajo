package com.sprint.otboo;

import com.sprint.otboo.weather.service.LocationNameResolver;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
	"GOOGLE_ID=test-google-id",
	"spring.mail.username=test@mail.com",
	"spring.mail.password=test"
})
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
