package com.sprint.otboo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void 사용자가_존재하는_경우_UserDetails_객체를_성공적으로_반환한다() {
        // given
        String userEmail = "test@abc.com";
        User mockUser = User.builder().email(userEmail).password("password1234").build();
        UserDto mockUserDto = new UserDto(UUID.randomUUID(), null, userEmail, "testuser", null, null, false);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserDto(mockUser)).thenReturn(mockUserDto);

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(mockUserDto.email());
        assertThat(userDetails.getPassword()).isEqualTo(mockUser.getPassword());
        verify(userRepository).findByEmail(userEmail);
    }

    @Test
    void 사용자가_존재하지_않는_경우_UsernameNotFoundException을_던진다() {
        // given
        String nonExistentEmail = "none@abc.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> customUserDetailsService.loadUserByUsername(nonExistentEmail));

        // then
        assertThat(thrown)
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining(nonExistentEmail);

        verify(userMapper, never()).toUserDto(any(User.class));
    }
}
