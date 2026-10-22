package com.tecnocampus.LS2.protube_back.auth;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthMailTest {
    @Test void sendsLinkAndReportsTransportFailuresWithoutLeakingTokens() {
        var sender = mock(JavaMailSender.class); var mail = new AuthMail(sender, "noreply@example.com");
        mail.send("user@example.com", "Verify", "https://example.com/#/verify?token=test");
        verify(sender).send(argThat((SimpleMailMessage message) -> message.getText().contains("token=test") && message.getTo()[0].equals("user@example.com")));
        doThrow(new MailSendException("secret transport detail")).when(sender).send(any(SimpleMailMessage.class));
        var error = assertThrows(AuthFailure.class, () -> mail.send("user@example.com", "Reset", "private-link"));
        assertEquals(503, error.status); assertFalse(error.getMessage().contains("private-link"));
    }
}
