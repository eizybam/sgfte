package mx.sgfte.core.notifications;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

final class EmailSender {

    private final Session session;

    EmailSender() {
        Properties props = new Properties();
        props.put("mail.smtp.host", MailConfig.HOST);
        props.put("mail.smtp.port", String.valueOf(MailConfig.PORT));
        props.put("mail.smtp.auth", String.valueOf(MailConfig.AUTH));
        props.put("mail.smtp.starttls.enable", String.valueOf(MailConfig.STARTTLS));

        this.session = MailConfig.AUTH
                ? Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MailConfig.USERNAME, MailConfig.PASSWORD);
            }
        })
                : Session.getInstance(props);
    }

    void send(String to, String subject, String body) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }
}