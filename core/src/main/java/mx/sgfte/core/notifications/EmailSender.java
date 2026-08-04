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

        /*
          Tiempos límite. Sin ellos, Jakarta Mail espera indefinidamente.

          Aquí eso no es un correo lento: los avisos salen por un único hilo, así
          que una conexión colgada lo bloquea para siempre y TODOS los avisos
          siguientes se quedan encolados detrás sin llegar nunca. Se comprobó:
          sin límite, un envío se quedó 45 segundos sin fallar ni registrar nada.

          Mejor un fallo en 10 segundos —que se registra en la bitácora y se
          puede consultar— que un silencio permanente.
         */
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

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
        // Remitente explícito: sin él, algunos servidores rechazan el mensaje.
        message.setFrom(new InternetAddress(MailConfig.FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }
}