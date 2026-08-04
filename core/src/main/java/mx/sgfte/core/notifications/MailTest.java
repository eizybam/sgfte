package mx.sgfte.core.notifications;

import mx.sgfte.core.notifications.EmailSender;

public class MailTest {
    public static void main(String[] args) throws Exception {
        new EmailSender().send("20253ds123@utez.edu.mx", "Prueba SGFTE", "Si ves esto, funcionó.");
        System.out.println("Enviado.");
    }
}