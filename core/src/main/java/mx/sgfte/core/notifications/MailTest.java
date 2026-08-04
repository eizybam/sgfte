package mx.sgfte.core.notifications;

/**
 * Diagnóstico del correo. Se ejecuta a mano:
 *
 *   java -cp <classpath> mx.sgfte.core.notifications.MailTest destino@ejemplo.com
 *
 * Prueba los DOS transportes y dice cuál funciona en esta red, en vez de
 * obligar a deducirlo desde un aviso que falló dentro de una transferencia.
 * Antes esto mandaba un correo a una dirección fija escrita en el código.
 */
public class MailTest {

    public static void main(String[] args) {
        String to = args.length > 0 ? args[0] : MailConfig.USERNAME;
        System.out.println("Servidor : " + MailConfig.HOST);
        System.out.println("Cuenta   : " + MailConfig.USERNAME);
        System.out.println("Destino  : " + to);
        System.out.println();

        try_(465, true,  "465 · SSL directo", to);
        try_(587, false, "587 · STARTTLS   ", to);

        System.out.println();
        System.out.println("Si funciona el 465, no hay nada que hacer: ya es el predeterminado.");
        System.out.println("Si sólo funciona el 587: SGFTE_MAIL_PORT=587 y SGFTE_MAIL_SSL=false.");
        System.out.println("Si fallan los dos por tiempo agotado, la red bloquea el correo saliente.");
    }

    private static void try_(int port, boolean ssl, String label, String to) {
        long started = System.currentTimeMillis();
        try {
            new EmailSender(port, ssl).send(to, "SGFTE · prueba de correo (" + label.trim() + ")",
                    "Si ves esto, este transporte funciona.");
            System.out.println("  [OK]    " + label + "  (" + (System.currentTimeMillis() - started) + " ms)");
        } catch (Exception e) {
            System.out.println("  [FALLA] " + label + "  " + EmailSender.explain(e));
        }
    }
}
