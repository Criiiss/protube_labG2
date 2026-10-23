package com.tecnocampus.LS2.protube_back.auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
@Component
public class AuthMail {
    private final JavaMailSender sender;
    private final String from;
    public AuthMail(JavaMailSender sender, @Value("${protube.auth.mail-from:no-reply@localhost}") String from) {
        this.sender = sender; this.from = from;
    }
    public void send(String email, String subject, String link) {
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject(subject);
        message.setText("Abre este enlace de protube:\n" + link + "\nSi no lo has solicitado, ignora este correo.");
        try { sender.send(message); }
        catch (org.springframework.mail.MailException ex) {
            throw new AuthFailure(503, "No se pudo enviar el correo. Inténtalo más tarde.");
        }
    }
}
