package org.help789.mds.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailSenderService {
    private final JavaMailSender sender;
    @Value("${spring.mail.username}")
    private String from;

    public MailSenderService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendLoginCode(String to, String code) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject("【您的登录验证码】");
            h.setText("<div>验证码：<b style='font-size:18px'>" + code +
                    "</b>，5分钟内有效。如非本人操作请忽略。</div>", true);
            sender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
