package mx.sgfte.core.shared.web;

import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The outcome of an operation, shown as the result modal (Figma 2169:436 for the
 * success case, 2169:542 for the rejection).
 *
 * Replaces the one-line flash that used to appear as text at the top of the
 * page. The prototype's card carries what actually matters after moving money —
 * how much, from where, to where, when — and a line of text cannot.
 *
 * This is for OPERATIONS, not for field validation. "El monto es obligatorio"
 * belongs next to the field the admin is still filling in, inside the form modal
 * they never left; showing it on a card with an amount and a destination that do
 * not exist yet would be nonsense, and it would throw away what they typed.
 *
 * Serializable because it crosses a redirect in the session.
 */
public class OperationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Session key. One at a time: a request produces a single outcome. */
    public static final String KEY = "operationResult";

    /*
      La fecha y la hora se formatean por separado, cada una con su idioma.

      El mes va en español porque la aplicación lo está; el AM/PM va en inglés
      porque es lo que dibuja el marco y porque es-MX lo escribe "a. m." con un
      espacio fino de no separación (U+202F) en medio: buscarlo y sustituirlo
      parece que funciona hasta que no funciona.
     */
    private static final DateTimeFormatter DATE_PART =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter TIME_PART =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final boolean ok;
    private final String title;
    private final String subtitle;
    private final String stripLabel;
    private final String stripText;
    private final List<Detail> details = new ArrayList<>();
    private String primaryLabel;
    private String primaryHref;
    private String secondaryLabel = "Volver al inicio";
    private String secondaryHref = "/admin/home";

    private OperationResult(boolean ok, String title, String subtitle,
                            String stripLabel, String stripText) {
        this.ok = ok;
        this.title = title;
        this.subtitle = subtitle;
        this.stripLabel = stripLabel;
        this.stripText = stripText;
    }

    public static OperationResult success(String title, String subtitle,
                                          String stripLabel, String stripText) {
        return new OperationResult(true, title, subtitle, stripLabel, stripText);
    }

    /**
     * A rejection. The strip is always headed "MOTIVO DEL RECHAZO" because on
     * this card the only thing worth reading is why it did not go through.
     */
    public static OperationResult rejected(String title, String subtitle, String reason) {
        return new OperationResult(false, title, subtitle, "MOTIVO DEL RECHAZO", reason);
    }

    public OperationResult detail(String label, String value) {
        if (value != null && !value.isBlank()) details.add(new Detail(label, value));
        return this;
    }

    /** Money as the card shows it: "$5,000.00 MXN". */
    public OperationResult amount(String label, BigDecimal value) {
        if (value == null) return this;
        return detail(label, "$" + String.format(Locale.US, "%,.2f", value) + " MXN");
    }

    /**
     * El sello de la tarjeta: "20 Ago 2026, 04:42 PM".
     *
     * Lo que llega es un LocalDateTime.now() del servlet, es decir la hora del
     * contenedor —UTC—, así que se traduce a la zona de la empresa antes de
     * escribirlo. Se hace aquí, en el único sitio por el que pasan las veinte
     * tarjetas de resultado, en vez de en cada llamador.
     */
    public OperationResult when(LocalDateTime moment) {
        if (moment == null) return this;
        // El mes sale en minúscula ("01 jul 2026"); el marco lo capitaliza.
        moment = mx.sgfte.core.shared.time.AppTime.displaySystem(moment);
        String date = DATE_PART.format(moment);
        int space = date.indexOf(' ');
        date = date.substring(0, space + 1)
             + Character.toUpperCase(date.charAt(space + 1))
             + date.substring(space + 2);
        return detail("Fecha", date + ", " + TIME_PART.format(moment));
    }

    public OperationResult primary(String label, String href) {
        this.primaryLabel = label;
        this.primaryHref = href;
        return this;
    }

    public OperationResult secondary(String label, String href) {
        this.secondaryLabel = label;
        this.secondaryHref = href;
        return this;
    }

    /**
     * Parks the outcome for the redirect that follows.
     *
     * Always a redirect and never a forward: these operations move money, and a
     * refresh must not repeat them. ResultFlashFilter picks it up on the way back
     * and clears it, so it shows exactly once.
     */
    public void flash(HttpSession session) {
        session.setAttribute(KEY, this);
    }

    public boolean isOk() { return ok; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getStripLabel() { return stripLabel; }
    public String getStripText() { return stripText; }
    public List<Detail> getDetails() { return details; }
    public String getPrimaryLabel() { return primaryLabel; }
    public String getPrimaryHref() { return primaryHref; }
    public String getSecondaryLabel() { return secondaryLabel; }
    public String getSecondaryHref() { return secondaryHref; }

    /** One line of the detail box: label on the left, value on the right. */
    public static class Detail implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final String value;

        Detail(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
    }
}
