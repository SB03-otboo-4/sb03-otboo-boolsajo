package com.sprint.otboo.auth.oauth;

import com.sprint.otboo.user.dto.data.UserDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final UserDto userDto;

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(UserDto userDto, Map<String, Object> attributes, String nameAttributeKey) {
        this.userDto = userDto;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role().name()));
    }

    @Override
    public String getName() {
        return attributes.get(nameAttributeKey).toString();
    }
}
