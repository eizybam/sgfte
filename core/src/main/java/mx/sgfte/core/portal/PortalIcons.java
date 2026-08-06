package mx.sgfte.core.portal;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Which icon a purpose gets on the cardholder's screens.
 *
 * The prototype draws a fuel pump for Gasolina, a plane for Viáticos and
 * cutlery for Comida. Categories are a catalogue the admin edits, though, so
 * the mapping has to survive names nobody anticipated — anything unmatched gets
 * the neutral tag rather than a wrong picture.
 *
 * It lives here, in Java, and not as a chain of c:choose in the JSP because two
 * screens need the same answer and a duplicated chain is a chain that drifts.
 */
public final class PortalIcons {

    private PortalIcons() {}

    /** El mismo icono de etiqueta que usa Categorías en el lateral del admin. */
    public static final String FALLBACK = "i-categories";

    public static String forPurpose(String purpose) {
        if (purpose == null) return FALLBACK;
        String key = strip(purpose);

        if (key.contains("gasolina") || key.contains("combustible") || key.contains("fuel")) {
            return "i-fuel";
        }
        if (key.contains("viatico") || key.contains("viaje") || key.contains("hospedaje")
                || key.contains("hotel") || key.contains("vuelo")) {
            return "i-plane";
        }
        if (key.contains("comida") || key.contains("aliment") || key.contains("restaurant")) {
            return "i-food";
        }
        return FALLBACK;
    }

    /** Lower-cased and without accents, so "Viáticos" matches "viatico". */
    private static String strip(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
