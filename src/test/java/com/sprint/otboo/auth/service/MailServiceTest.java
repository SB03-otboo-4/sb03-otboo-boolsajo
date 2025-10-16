package com.sprint.otboo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.auth.util.MailService;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.MailSendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "no-reply@abc.com");
    }

    @Test
    void 임시_비밀번호_메일을_성공적으로_전송한다() {
        // given
        String toEmail = "test@example.com";
        String tempPassword = "tempPassword123!";

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        // when
        mailService.sendTemporaryPasswordEmail(toEmail, tempPassword);

        // then
        verify(mailSender, times(1)).send(mimeMessageCaptor.capture());
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();
        assertThat(capturedMessage).isEqualTo(mimeMessage);
    }

    @Test
    void 메일_전송_중_오류가_발생하면_MailSendFailedException을_던진다() {
        // given
        String toEmail = "test@abc.com";
        String tempPassword = "tempPassword123!";

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("Test Exception")).when(mailSender).send(any(MimeMessage.class));

        // when
        Throwable thrown = catchThrowable(() -> mailService.sendTemporaryPasswordEmail(toEmail, tempPassword));

        // then
        assertThat(thrown)
            .isInstanceOf(MailSendFailedException.class)
            .hasMessage(ErrorCode.MAIL_SEND_FAILED.getMessage());
    }
}
