package com.hotelnt.helper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpEmailSenderTest {
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> senders = mock(ObjectProvider.class);

    @Test
    void sendsCodeToTheRequestedAddress() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(senders.getIfAvailable()).thenReturn(sender);
        new OtpEmailSender(senders, "noreply@example.com").send("host@example.com", "001234");
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertEquals("noreply@example.com", message.getValue().getFrom());
        assertEquals("host@example.com", message.getValue().getTo()[0]);
        assertTrue(message.getValue().getText().contains("001234"));
    }

    @Test
    void failsWhenSmtpIsNotConfigured() {
        OtpEmailSender sender = new OtpEmailSender(senders, "noreply@example.com");
        assertThrows(IllegalStateException.class, () -> sender.send("host@example.com", "001234"));
    }
}