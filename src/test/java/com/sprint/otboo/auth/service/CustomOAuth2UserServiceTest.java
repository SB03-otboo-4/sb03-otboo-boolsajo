package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.auth.oauth.CustomOAuth2User;
import com.sprint.otboo.auth.oauth.CustomOAuth2UserService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private ClientRegistration kakaoClientRegistration;
    private ClientRegistration googleClientRegistration;

    @BeforeEach
    void setUp() {
        kakaoClientRegistration = ClientRegistration.withRegistrationId("kakao")
            .userNameAttributeName("id")
            .clientId("kakao-client-id")
            .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("profile_nickname", "account_email")
            .authorizationUri("https://kauth.kakao.com/oauth/authorize")
            .tokenUri("https://kauth.kakao.com/oauth/token").userInfoUri("https://kapi.kakao.com/v2/user/me")
            .build();

        googleClientRegistration = ClientRegistration.withRegistrationId("google")
            .userNameAttributeName("sub")
            .clientId("google-client-id")
            .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v1/userinfo")
            .build();
    }

    private OAuth2UserRequest createOAuth2UserRequest(ClientRegistration clientRegistration, Map<String, Object> attributes) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-token", Instant.now(), Instant.now().plusSeconds(3600));
        return new OAuth2UserRequest(clientRegistration, accessToken, attributes);
    }

    @Test
    void 카카오소셜로그인_신규가입() {
        // given
        Map<String, Object> attributes = Map.of(
            "id", "123456789",
            "properties", Map.of("nickname", "테스트유저"),
            "kakao_account", Map.of("email", null)
        );
        OAuth2UserRequest userRequest = createOAuth2UserRequest(kakaoClientRegistration, attributes);

        when(userRepository.findByProviderAndProviderUserId(LoginType.KAKAO, "123456789")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserDto(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            return new UserDto(
                UUID.randomUUID(),
                user.getCreatedAt(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                user.getLocked());
        });

        // when
        CustomOAuth2User resultUser = (CustomOAuth2User) customOAuth2UserService.loadUser(userRequest);

        // then
        assertThat(resultUser).isNotNull();
        UserDto resultUserDto = resultUser.getUserDto();
        assertThat(resultUserDto.email()).isEqualTo("테스트유저@kakao.com");
        assertThat(resultUserDto.name()).isEqualTo("테스트유저");
        assertThat(resultUserDto.role()).isEqualTo(Role.USER);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void 구글소셜로그인_기존유저_성공() {
        // given
        Map<String, Object> attributes = Map.of(
            "sub", "987654321",
            "name", "기존유저",
            "email", "googleuser@gmail.com"
        );
        OAuth2UserRequest userRequest = createOAuth2UserRequest(googleClientRegistration, attributes);

        User existingUser = User.builder()
            .id(UUID.randomUUID())
            .username("기존유저")
            .email("googleuser@gmail.com")
            .provider(LoginType.GOOGLE)
            .providerUserId("987654321")
            .role(Role.USER)
            .build();

        UserDto existingUserDto = new UserDto(existingUser.getId(), existingUser.getCreatedAt(), existingUser.getUsername(), existingUser.getEmail(), existingUser.getRole(), existingUser.getProvider(), existingUser.getLocked());

        when(userRepository.findByProviderAndProviderUserId(LoginType.GOOGLE, "987654321")).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserDto(existingUser)).thenReturn(existingUserDto);


        // when
        CustomOAuth2User resultUser = (CustomOAuth2User) customOAuth2UserService.loadUser(userRequest);

        // then
        assertThat(resultUser).isNotNull();
        UserDto resultUserDto = resultUser.getUserDto();
        assertThat(resultUserDto.email()).isEqualTo("googleuser@gmail.com");
        assertThat(resultUserDto.id()).isEqualTo(existingUser.getId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 카카오소셜로그인_실패_ID누락() {
        // given
        Map<String, Object> attributes = Map.of(
            "properties", Map.of("nickname", "테스트유저")
        );
        OAuth2UserRequest userRequest = createOAuth2UserRequest(kakaoClientRegistration, attributes);

        // when
        Throwable thrown = catchThrowable(() -> {
            customOAuth2UserService.loadUser(userRequest);
        });

        // then
        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(NullPointerException.class);
    }
}