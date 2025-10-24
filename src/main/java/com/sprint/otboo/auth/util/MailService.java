package com.sprint.otboo.auth.util;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.auth.MailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTemporaryPasswordEmail(String toEmail, String tempPassword) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[OTBOO] 임시 비밀번호가 발급되었습니다.");

            String htmlContent = buildEmailContent(tempPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("메일 전송 실패: toEmail={}, error={}", toEmail, e.getMessage());
            throw new MailSendFailedException(ErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    private String buildEmailContent(String tempPassword) {
        return "<html><body>" +
            "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
            "<h1 style='color: #333;'>[OTBOO] 임시 비밀번호 안내</h1>" +
            "<p>안녕하세요!</p>" +
            "<p>요청하신 임시 비밀번호가 발급되었습니다. 아래 비밀번호를 사용하여 로그인 후, 즉시 새로운 비밀번호로 변경해주세요.</p>" +
            "<div style='background-color: #f2f2f2; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;'>" +
            "<h2 style='margin: 0; color: #0056b3; letter-spacing: 2px;'>" + tempPassword + "</h2>" +
            "</div>" +
            "<div style='background-color: #fffbe6; border: 1px solid #ffeeba; padding: 15px; border-radius: 5px;'>" +
            "<h3 style='margin-top: 0;'> 중요 안내사항</h3>" +
            "<ul>" +
            "<li>이 임시 비밀번호는 <strong>3분간만 유효</strong>합니다.</li>" +
            "<li>보안을 위해 로그인 후 즉시 새로운 비밀번호로 변경해주세요.</li>" +
            "<li>임시 비밀번호는 다른 사람과 공유하지 마세요.</li>" +
            "</ul>" +
            "</div>" +
            "<p style='margin-top: 30px; font-size: 0.9em; color: #777;'>본 메일은 발신전용이므로 회신되지 않습니다.</p>" +
            "</div>" +
            "</body></html>";
    }
}
