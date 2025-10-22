package com.sprint.otboo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.service.impl.UserQueryServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserQueryServiceImpl 요약 조회 테스트")
@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserQueryServiceImpl service;

    @Test
    @DisplayName("요약_정상조회_성공")
    void 요약_정상조회_성공() {
        // given
        UUID id = UUID.randomUUID();
        User user = mock(User.class);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn("woody");
        when(user.getProfileImageUrl()).thenReturn("https://s3/img.png");

        // when
        UserSummaryResponse resp = service.getSummary(id);

        // then
        assertThat(resp.userId()).isEqualTo(id);
        assertThat(resp.name()).isEqualTo("woody");
        assertThat(resp.profileImageUrl()).isEqualTo("https://s3/img.png");
        verify(userRepository).findById(id);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("요약_사용자없음_예외")
    void 요약_사용자없음_예외() {
        // given
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> service.getSummary(id))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage())
            .extracting(ex -> ((CustomException) ex).getErrorCode())
            .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(id);
        verifyNoMoreInteractions(userRepository);
    }
}
