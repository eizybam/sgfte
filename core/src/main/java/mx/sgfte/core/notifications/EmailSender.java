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

    EmailSender() { this(MailConfig.PORT, MailConfig.SSL); }

    /**
     * Permite fijar puerto y modo, que es lo que necesita el diagnóstico para
     * probar los dos transportes sin tocar la configuración.
     */
    EmailSender(int port, boolean implicitSsl) {
        Properties props = new Properties();
        props.put("mail.smtp.host", MailConfig.HOST);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(MailConfig.AUTH));

        /*
          Dos formas de cifrar, y la red decide cuál se puede usar:

            · 465 con SSL desde el saludo inicial (implicitSsl).
            · 587 en claro y luego STARTTLS.

          Muchas redes de escuela y de oficina filtran el 587: la conexión TCP
          abre, pero la negociación de STARTTLS no recibe respuesta y acaba en
          "Could not convert socket to TLS" con un Read timed out debajo. Por eso
          se puede cambiar de transporte sin tocar código.
         */
        if (implicitSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Explícito: si el servidor y el JDK no acuerdan versión, el error que
        // sale es igual de opaco que el del filtrado.
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        /*
          Tiempos límite. Sin ellos, Jakarta Mail espera indefinidamente.

          Aquí eso no es un correo lento: los avisos salen por un único hilo, así
          que una conexión colgada lo bloquea para siempre y TODOS los avisos
          siguientes se quedan encolados detrás sin llegar nunca. Se comprobó:
          sin límite, un envío se quedó 45 segundos sin fallar ni registrar nada.
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

    /**
     * La cadena de causas, en una línea.
     *
     * MessagingException envuelve el error de verdad: el mensaje de arriba dice
     * "Could not convert socket to TLS" y el de abajo, que es el que sirve,
     * dice si fue un tiempo agotado, un certificado o un rechazo de la cuenta.
     */
    static String explain(Throwable error) {
        StringBuilder chain = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (chain.length() > 0) chain.append(" <- ");
            chain.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
        }
        return chain.toString();
    }
}
