package com.sprint.otboo.auth.oauth;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final DefaultOAuth2UserService delegate;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        LoginType provider = LoginType.valueOf(registrationId.toUpperCase());

        OAuth2UserInfo userInfo;
        if (provider == LoginType.GOOGLE) {
            userInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (provider == LoginType.KAKAO) {
            userInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
            userInfo = null;
        }

        String providerUserId = userInfo.getProviderUserId();
        String email = userInfo.getEmail();

        // DB에서 사용자 조회
        User user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
            .orElseGet(() -> {
                // 사용자가 없으면 새로 생성 (회원가입)
                User newUser = User.builder()
                    .email(email)
                    .username(userInfo.getName())
                    .role(Role.USER)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .build();
                User savedUser = userRepository.save(newUser);

                UserProfile profile = UserProfile.builder()
                    .user(savedUser)
                    .build();
                userProfileRepository.save(profile);

                return savedUser;
            });

        UserDto userDto = userMapper.toUserDto(user);

        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new CustomOAuth2User(userDto, oAuth2User.getAttributes(), userNameAttributeName);
    }
}
