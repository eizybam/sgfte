package mx.sgfte.core.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The employee code rule, tested without a database — the sequence value is the
 * only thing the real thing needs from Oracle, and it is passed in.
 */
class EmployeeCodeTest {

    @Test
    @DisplayName("reproduces the prototype's example: Diego Jarillo Estrada -> DJE0077")
    void prototypeExample() {
        assertEquals("DJE0077", EmployeeCode.of("Diego Jarillo Estrada", 77));
    }

    @Test
    @DisplayName("takes one initial per word, up to three")
    void initialsPerWord() {
        assertEquals("SR", EmployeeCode.initials("Sofia Rodriguez"));
        assertEquals("MG", EmployeeCode.initials("Maria Gonzalez"));
        assertEquals("JPL", EmployeeCode.initials("Juan Perez Lopez"));
    }

    @Test
    @DisplayName("stops at three initials even with more surnames")
    void capsAtThree() {
        assertEquals("ABC", EmployeeCode.initials("Ana Bernal Cortes Delgado"));
    }

    @Test
    @DisplayName("strips accents so the code stays dictatable")
    void stripsAccents() {
        assertEquals("AN", EmployeeCode.initials("Ángel Ñuño"));
        assertEquals("JPL", EmployeeCode.initials("José Pérez Láinez"));
    }

    @Test
    @DisplayName("a single word falls back to its first two letters")
    void singleWord() {
        assertEquals("CH", EmployeeCode.initials("Cher"));
    }

    @Test
    @DisplayName("pads the sequence to four digits")
    void padsToFour() {
        assertEquals("SR0001", EmployeeCode.of("Sofia Rodriguez", 1));
        assertEquals("SR0999", EmployeeCode.of("Sofia Rodriguez", 999));
        assertEquals("SR9999", EmployeeCode.of("Sofia Rodriguez", 9999));
    }

    @Test
    @DisplayName("past 9999 the code grows rather than wrapping — a repeat would be worse")
    void growsBeyondFourDigits() {
        assertEquals("SR10000", EmployeeCode.of("Sofia Rodriguez", 10000));
    }

    @Test
    @DisplayName("same initials still yield different codes: uniqueness is the number's job")
    void sameInitialsDifferentCodes() {
        String first = EmployeeCode.of("Juan Perez Lopez", 10);
        String second = EmployeeCode.of("Juan Perez Lopez", 11);
        assertEquals("JPL0010", first);
        assertEquals("JPL0011", second);
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("ignores digits and punctuation inside the name")
    void ignoresNonLetters() {
        assertEquals("MG", EmployeeCode.initials("María  (2) Gonzalez"));
    }

    @Test
    @DisplayName("an empty name yields no initials rather than blowing up")
    void emptyName() {
        assertEquals("", EmployeeCode.initials(""));
        assertEquals("", EmployeeCode.initials(null));
    }

    @Test
    @DisplayName("splitName keeps the first word as the given name and the rest as surnames")
    void splitsFullName() {
        assertArrayEquals(new String[]{"Diego", "Jarillo Estrada"},
                CardholderService.splitName("Diego Jarillo Estrada"));
        assertArrayEquals(new String[]{"Sofia", "Rodriguez"},
                CardholderService.splitName("  Sofia   Rodriguez  "));
    }

    @Test
    @DisplayName("splitName rejects a one-word name: last_name is NOT NULL")
    void rejectsSingleWordName() {
        assertNull(CardholderService.splitName("Cher"));
        assertNull(CardholderService.splitName("   "));
        assertNull(CardholderService.splitName(null));
    }
}
