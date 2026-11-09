package com.hotelnt.modules.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Gui OTP qua SMTP; thong tin dang nhap duoc cau hinh qua moi truong. */
@Component
class OtpEmailSender {
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;

    OtpEmailSender(ObjectProvider<JavaMailSender> senders,
                   @Value("${app.auth.mail-from:${spring.mail.username:}}") String from) {
        this.senders = senders;
        this.from = from;
    }

    void send(String recipient, String otp) {
        if (recipient == null || recipient.isBlank() || otp == null || !otp.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Invalid OTP recipient or code");
        }
        JavaMailSender sender = senders.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            throw new IllegalStateException("SMTP is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Hotel Nha Trang - Mã xác thực OTP");
        message.setText("Mã OTP của bạn là: " + otp
                + ". Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này.");
        sender.send(message);
    }

    boolean isConfigured() {
        return from != null && !from.isBlank() && senders.getIfAvailable() != null;
    }
}
