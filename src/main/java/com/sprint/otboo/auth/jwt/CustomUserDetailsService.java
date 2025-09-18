package com.sprint.otboo.auth.jwt;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));
        UserDto userDto = userMapper.toUserDto(user);

        return new CustomUserDetails(
            userDto,
            user.getPassword());
    }
}
