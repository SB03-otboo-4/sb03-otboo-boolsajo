package com.sprint.otboo.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Gender;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toUserDto_기본_필드가_정상_매핑() {
        // given
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00.00Z");
        User user = User.builder()
            .id(userId)
            .createdAt(createdAt)
            .username("testUser")
            .email("test@test.com")
            .role(Role.USER)
            .locked(true)
            .provider(LoginType.KAKAO)
            .build();

        // when
        UserDto dto = mapper.toUserDto(user);

        // then
        assertThat(dto.id()).isEqualTo(userId);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.name()).isEqualTo("testUser");
        assertThat(dto.linkedOAuthProviders()).isEqualTo(LoginType.KAKAO);
        assertThat(dto.locked()).isTrue();
    }

    @Test
    void toProfileDto_위지정보를_리스트로_분리() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .createdAt(Instant.parse("2024-01-01T03:00:00Z"))
            .username("testUser")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .build();
        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(user)
            .gender(Gender.MALE)
            .birthDate(LocalDate.parse("1998-09-21"))
            .latitude(new BigDecimal("37.4875493"))
            .longitude(new BigDecimal("126.6849254"))
            .x(55)
            .y(125)
            .locationNames("인천광역시 서구 가좌동")
            .temperatureSensitivity(5)
            .build();

        // when
        ProfileDto dto = mapper.toProfileDto(user, profile);

        // then
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.gender()).isEqualTo(Gender.MALE);
        assertThat(dto.location()).isNotNull();
        assertThat(dto.location().locationNames())
            .containsExactly("인천광역시","서구","가좌동");
    }

    @Test
    void toProfileDto_위치정보가_없으면_null을_반환() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .createdAt(Instant.now())
            .username("testUser")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .build();
        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(user)
            .temperatureSensitivity(null)
            .build();

        // when
        ProfileDto dto = mapper.toProfileDto(user, profile);

        // then
        assertThat(dto.location()).isNull();
    }

    @Test
    void toProfileDto_Profile만으로_필드를_매핑() {
        // given
        UUID userId = UUID.randomUUID();
        User userEntity = User.builder()
            .id(userId)
            .createdAt(Instant.parse("2025-09-28T00:00:00Z"))
            .username("testUser")
            .email("test@test.com")
            .role(Role.USER)
            .locked(false)
            .build();
        UserProfile profile = UserProfile.builder()
            .userId(userId)
            .user(userEntity)
            .gender(Gender.OTHER)
            .locationNames("인천광역시 서구 가좌동")
            .build();

        // when
        ProfileDto dto = mapper.toProfileDto(profile);

        // then
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.name()).isEqualTo("testUser");
        assertThat(dto.location().locationNames()).containsExactly("인천광역시","서구","가좌동");
    }

    @Test
    void convertLocationNames_복수_공백_정상_분리() {
        // given
        String raw = "인천광역시     서구  가좌동";

        // when
        List<String> tokens = mapper.convertLocationNames(raw);

        // then
        assertThat(tokens).containsExactly("인천광역시","서구","가좌동");
    }
}
