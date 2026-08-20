package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;

import jakarta.mail.MessagingException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Avisa al tarjetahabiente cuando su cuenta recibe dinero.
 *
 * Funciona como un microservicio: nada de lo que pase aquí puede afectar a la
 * operación que lo disparó. Eso se sostiene sobre tres decisiones, y las tres
 * importan:
 *
 *   1. Se llama DESPUÉS del commit. Notificar dentro de la transacción metería
 *      una conexión SMTP —segundos, a un servidor de terceros— dentro de un
 *      bloqueo de base de datos, y un fallo de correo desharía una
 *      transferencia que ya era válida.
 *   2. Se manda en OTRO hilo. Aunque ya no pueda deshacer nada, esperar al
 *      correo dejaría al usuario mirando el navegador mientras Gmail responde.
 *   3. No propaga NADA. Ni excepciones ni errores: se registran en la bitácora
 *      y ahí termina. El dinero ya se movió; que el aviso falle es un problema
 *      menor y separado.
 */
public class NotificationService {

    /*
      Un solo hilo, demonio y compartido.

      Daemon para que no impida apagar el servidor. Uno solo porque el volumen
      es de avisos sueltos, y encolarlos evita abrir una conexión SMTP por cada
      movimiento simultáneo.
     */
    private static final ExecutorService DISPATCH = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sgfte-notifications");
        t.setDaemon(true);
        return t;
    });

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX"));

    private final AuditLogService auditLogService = new AuditLogService();
    private final NotificationDao dao = new NotificationDao();
    private final NotificationLogDao logDao = new NotificationLogDao();
    private final EmailSender emailSender = new EmailSender();

    /**
     * Una cuenta recibió dinero: avisa a su dueño.
     *
     * @param destinationAccountId la cuenta que recibe
     * @param sourceAccountId      la cuenta que envía, o null si vino de la
     *                             Concentradora (una dispersión no sale de la
     *                             cuenta de nadie)
     * @param amount               lo recibido
     * @param concept              el concepto que se escribió al mover el dinero
     */
    public void moneyReceived(long destinationAccountId, Long sourceAccountId,
                              BigDecimal amount, String concept) {
        // La llamada vuelve aquí de inmediato: lo demás ocurre en el otro hilo.
        DISPATCH.submit(() -> {
            try {
                deliverMoneyReceived(destinationAccountId, sourceAccountId, amount, concept);
            } catch (RuntimeException e) {
                // El submit se traga las excepciones del Runnable; se registran
                // para que no desaparezcan en silencio.
                System.err.println("[NOTIFY] aviso de ingreso fallido: " + e.getMessage());
            }
        });
    }

    private void deliverMoneyReceived(long destinationAccountId, Long sourceAccountId,
                                      BigDecimal amount, String concept) {
        AccountParty recipient = dao.findParty(destinationAccountId).orElse(null);
        if (recipient == null) {
            System.err.println("[NOTIFY] cuenta " + destinationAccountId + " sin destinatario");
            return;
        }
        if (recipient.holderEmail() == null || recipient.holderEmail().isBlank()) {
            System.err.println("[NOTIFY] " + recipient.holderName() + " no tiene correo");
            return;
        }

        String sender = sourceAccountId == null
                ? "Cuenta Concentradora"
                : dao.findParty(sourceAccountId)
                     .map(p -> p.holderName() + " (" + p.label() + ")")
                     .orElse("Otra cuenta");

        // Dispersión si vino de la Concentradora, transferencia si vino de un
        // compañero — la misma distinción que ya se hacía para "sender".
        AuditEvent event = sourceAccountId == null ? AuditEvent.DISPERSION : AuditEvent.TRANSFER;

        deliver(recipient.cardholderId(), event, recipient.holderEmail(),
                "Recibiste " + money(amount) + " en tu cuenta " + recipient.purpose(),
                body(recipient, sender, amount, concept), null);
    }

    /** El texto del correo. Plano a propósito: se lee igual en cualquier cliente. */
    private String body(AccountParty recipient, String sender, BigDecimal amount, String concept) {
        return "Hola " + recipient.holderName() + ",\n\n"
             + "Tu cuenta acaba de recibir fondos.\n\n"
             + "  Monto recibido : " + money(amount) + "\n"
             + "  De             : " + sender + "\n"
             + "  Cuenta destino : " + recipient.label() + "\n"
             + "  Concepto       : " + (concept == null || concept.isBlank() ? "Sin concepto" : concept) + "\n"
             + "  Saldo actual   : " + money(recipient.balance()) + "\n"
             + "  Fecha          : " + STAMP.format(LocalDateTime.now()) + "\n\n"
             + "Puedes consultar el detalle en tu panel de SGFTE.\n\n"
             + "— Sistema de Gestión de Fondos y Tarjetas Empresariales";
    }

    private String money(BigDecimal amount) {
        return amount == null ? "$0.00 MXN"
                : "$" + String.format(Locale.US, "%,.2f", amount) + " MXN";
    }

    /**
     * Punto de entrada para cualquier otro disparador — alta/baja de cuenta,
     * tarjeta emitida, login, cambio de contraseña... — que quiera avisar por
     * correo Y dejar la fila que el Notificaciones del tarjetahabiente lee.
     *
     * Mismo hilo único que moneyReceived: la llamada vuelve de inmediato, el
     * envío y el registro ocurren detrás.
     */
    public void notify(long cardholderId, AuditEvent event, String to, String subject, String body) {
        notify(cardholderId, event, to, subject, body, null);
    }

    /** Igual, con versión HTML opcional del mismo mensaje (ver EmailSender.send). */
    public void notify(long cardholderId, AuditEvent event, String to, String subject,
                       String body, String html) {
        DISPATCH.submit(() -> {
            try {
                deliver(cardholderId, event, to, subject, body, html);
            } catch (RuntimeException e) {
                System.err.println("[NOTIFY] aviso fallido: " + e.getMessage());
            }
        });
    }

    /**
     * El envío y el registro, ya en el hilo de despacho — deliverMoneyReceived
     * y notify() confluyen aquí para no envolver un submit dentro de otro.
     */
    private void deliver(long cardholderId, AuditEvent event, String to, String subject,
                         String body, String html) {
        sendEmail(to, subject, body, html);
        // Se registra en la pantalla del tarjetahabiente pase lo que pase con el
        // correo: el evento (se emitió una tarjeta, entró dinero...) ocurrió de
        // verdad aunque Gmail esté teniendo un mal día; el registro en pantalla
        // es la fuente de verdad, el correo es sólo el canal de aviso.
        logDao.insert(cardholderId, event, subject);
    }

    /**
     * Envío directo, sin registrar en el Notificaciones de nadie. Sigue siendo
     * público porque ya lo usaban otras piezas, y porque un aviso suelto no
     * siempre nace de un evento de un cardholder concreto.
     *
     * Registra el intento en la bitácora salga bien o mal: un aviso que no llegó
     * es justo lo que hay que poder consultar después.
     */
    public void send(String to, String subject, String body) {
        sendEmail(to, subject, body, null);
    }

    /** Igual, con versión HTML opcional del mismo mensaje (ver EmailSender.send). */
    public void send(String to, String subject, String body, String html) {
        sendEmail(to, subject, body, html);
    }

    private void sendEmail(String to, String subject, String body, String html) {
        try {
            emailSender.send(to, subject, body, html);
            auditLogService.record(AuditEvent.NOTIFICATION, subject + " -> " + to, "system", null);
        } catch (MessagingException | RuntimeException e) {
            // La causa real y no sólo "Could not convert socket to TLS", que no
            // dice si fue la red, el certificado o la contraseña.
            String why = EmailSender.explain(e);
            System.err.println("[NOTIFY][ERROR] No se pudo enviar a " + to + ": " + why);
            auditLogService.record(AuditEvent.NOTIFICATION,
                    subject + " -> " + to + " [FALLÓ: " + why + "]", "system", null);
        }
    }
}
