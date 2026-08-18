package mx.sgfte.core.funding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las CLABEs de estas pruebas NO salen de Clabe.checkDigit(): se calcularon
 * aparte, con una implementación distinta del algoritmo publicado, y se pegaron
 * aquí como constantes.
 *
 * Es a propósito. Si el fixture se generara con el mismo código que se está
 * probando, la prueba pasaría igual de bien con el algoritmo mal implementado —
 * sólo demostraría que la clase es consistente consigo misma, que es justo lo
 * que no interesa saber.
 */
class ClabeTest {

    // 3 institución + 3 plaza + 11 cuenta + 1 control.
    private static final String BBVA      = "012180000000000002";
    private static final String BANAMEX   = "002180000123456789";
    private static final String BANORTE   = "072180000987654320";
    private static final String SANTANDER = "014180000555555550";
    private static final String STP       = "642180000111111111";

    @Test
    @DisplayName("acepta CLABEs con dígito de control correcto")
    void aceptaValidas() {
        for (String clabe : new String[] { BBVA, BANAMEX, BANORTE, SANTANDER, STP }) {
            assertTrue(Clabe.isValid(clabe), "debió aceptar " + clabe);
        }
    }

    @Test
    @DisplayName("rechaza una CLABE con el dígito de control cambiado")
    void rechazaDigitoDeControlMalo() {
        // Mismos 17 dígitos, último distinto: es el error que el dígito existe
        // para atrapar, y el que comete quien teclea la CLABE a mano.
        assertFalse(Clabe.isValid("012180000000000003"));
        assertFalse(Clabe.isValid("002180000123456780"));
    }

    @Test
    @DisplayName("rechaza dos dígitos intercambiados")
    void rechazaTransposicion() {
        // 0123456789 -> 0123456798 en la parte de cuenta. Los ponderadores
        // 3-7-1 existen precisamente para que una transposición no cuadre.
        assertTrue(Clabe.isValid(BANAMEX));
        assertFalse(Clabe.isValid("002180000123456879"));
    }

    @Test
    @DisplayName("calcula el dígito de control de los primeros 17")
    void calculaDigito() {
        assertEquals(2, Clabe.checkDigit("01218000000000000"));
        assertEquals(9, Clabe.checkDigit("00218000012345678"));
        assertEquals(0, Clabe.checkDigit("07218000098765432"));
        assertEquals(0, Clabe.checkDigit("01418000055555555"));
        assertEquals(1, Clabe.checkDigit("64218000011111111"));
    }

    @Test
    @DisplayName("rechaza dieciocho ceros aunque el checksum cuadre")
    void rechazaCerosAunqueCuadre() {
        // 0*3 + 0*7 + ... = 0, y el dígito de control de 0 es 0: el checksum la
        // aprueba. Es justo lo que se teclea para rellenar el campo y seguir.
        assertEquals(0, Clabe.checkDigit("00000000000000000"));
        assertFalse(Clabe.isValid("000000000000000000"));
    }

    @Test
    @DisplayName("rechaza lo que ni siquiera tiene forma de CLABE")
    void rechazaBasura() {
        assertFalse(Clabe.isValid(null));
        assertFalse(Clabe.isValid(""));
        assertFalse(Clabe.isValid("012"));                    // corta
        assertFalse(Clabe.isValid("0121800000000000021"));    // larga
        assertFalse(Clabe.isValid("01218000000000000X"));     // no es dígito
        assertFalse(Clabe.isValid("no soy una clabe!!"));     // 18 caracteres, pero no dígitos
    }

    @Test
    @DisplayName("tolera los espacios y guiones del comprobante del banco")
    void toleraSeparadores() {
        // Se copia y pega desde el CEP o desde la ficha, y viene separada.
        assertTrue(Clabe.isValid("012 180 00000000000 2"));
        assertTrue(Clabe.isValid("012-180-00000000000-2"));
        assertEquals(BBVA, Clabe.normalize("012 180 00000000000 2"));
    }

    @Test
    @DisplayName("normalize devuelve null cuando no hay CLABE que normalizar")
    void normalizeNull() {
        assertNull(Clabe.normalize(null));
        assertNull(Clabe.normalize("012"));
        assertNull(Clabe.normalize("01218000000000000X"));
    }

    @Test
    @DisplayName("expone la institución y una forma legible")
    void institucionYFormato() {
        assertEquals("012", Clabe.bankCode(BBVA));
        assertEquals("642", Clabe.bankCode(STP));
        assertEquals("012 180 00000000000 2", Clabe.format(BBVA));
        // Si no es CLABE, se devuelve tal cual en vez de reventar en una pantalla.
        assertEquals("nope", Clabe.format("nope"));
    }
}
