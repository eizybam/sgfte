package mx.sgfte.core.users;

import java.text.Normalizer;

/**
 * Builds the employee code shown across the admin screens: initials of the full
 * name plus a four-digit number — "Diego Jarillo Estrada" → "DJE0077".
 *
 * The letters come from the NAME, not the email. The prototype's own sample
 * email (dje777@sgfte.mx) is itself derived from the name, and emails change
 * — people marry, domains move — while an identifier must not.
 *
 * The letters alone cannot be unique: two "Juan Pérez López" both yield JPL.
 * Uniqueness comes from the number, which is a database sequence, so there is
 * no collision to retry and no birthday problem to reason about.
 *
 * Nothing here reads the database, so the rule is unit-testable on its own.
 */
public final class EmployeeCode {

    /** Cuatro dígitos: 9,999 altas antes de tener que ensanchar la columna. */
    private static final int DIGITS = 4;

    /** Como mucho tres iniciales: nombre + dos apellidos, la convención local. */
    private static final int MAX_INITIALS = 3;

    private EmployeeCode() {}

    /**
     * @param fullName  el nombre tal cual lo escribió el admin
     * @param sequence  valor de seq_employee_code; sólo aporta unicidad
     */
    public static String of(String fullName, long sequence) {
        return initials(fullName) + pad(sequence);
    }

    /**
     * First letter of each word, up to three, without accents.
     *
     * A name with a single word still needs at least two letters for the code to
     * look like the others, so it falls back to that word's first two letters.
     */
    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";

        String[] words = stripAccents(fullName).trim().split("\\s+");
        StringBuilder out = new StringBuilder();

        for (String word : words) {
            if (out.length() == MAX_INITIALS) break;
            String letters = word.replaceAll("[^A-Za-z]", "");
            if (!letters.isEmpty()) out.append(Character.toUpperCase(letters.charAt(0)));
        }

        if (out.length() == 1) {
            String first = stripAccents(words[0]).replaceAll("[^A-Za-z]", "");
            if (first.length() >= 2) out.append(Character.toUpperCase(first.charAt(1)));
        }
        return out.toString();
    }

    /**
     * Zero-pads the sequence. Past 9,999 it simply grows rather than wrapping —
     * a longer code is harmless; a repeated one would not be.
     */
    private static String pad(long sequence) {
        String digits = Long.toString(sequence);
        return digits.length() >= DIGITS ? digits : "0".repeat(DIGITS - digits.length()) + digits;
    }

    /** "Ñuño" -> "Nuno": el código se dicta en voz alta y se teclea. */
    private static String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                         .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
