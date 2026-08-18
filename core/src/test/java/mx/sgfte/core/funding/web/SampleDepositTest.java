package mx.sgfte.core.funding.web;

import mx.sgfte.core.funding.Clabe;
import mx.sgfte.core.funding.FundingDeposit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El generador del simulador tiene que producir SIEMPRE datos que la validación
 * acepte. Si no, deja de ahorrar trabajo y pasa a inventarlo: se pulsa
 * "generar", se publica, y el depósito se rechaza por algo que el usuario no
 * escribió.
 *
 * Se generan cientos de muestras en cada prueba y no una, porque los fallos de
 * un generador aleatorio son por definición intermitentes. La primera versión
 * de esta clase reventaba con "bound must be greater than origin" en el RFC —
 * un rango invertido— y una sola muestra podría haberlo dejado pasar según la
 * rama que tocara. Doscientas no.
 */
class SampleDepositTest {

    private static final int SAMPLES = 200;

    @Test
    @DisplayName("un SPEI generado trae CLABE ordenante válida de verdad")
    void speiGeneraClabeValida() {
        for (int i = 0; i < SAMPLES; i++) {
            Map<String, String> d = SampleDeposit.spei();
            String clabe = d.get("ordenanteClabe");
            assertTrue(Clabe.isValid(clabe),
                    "el generador produjo una CLABE que el validador rechaza: " + clabe);
        }
    }

    @Test
    @DisplayName("la CLABE generada empieza por el código del banco que dice ser")
    void clabeYBancoConcuerdan() {
        // Una CLABE que abre en 012 junto a un rótulo que dice BANORTE se nota a
        // la primera y estropea la demostración.
        Set<String> codes = Set.of("002", "012", "014", "021", "030", "036",
                                   "044", "058", "062", "072", "127", "646");
        for (int i = 0; i < SAMPLES; i++) {
            Map<String, String> d = SampleDeposit.spei();
            assertTrue(codes.contains(Clabe.bankCode(d.get("ordenanteClabe"))),
                    "código de banco inesperado en " + d.get("ordenanteClabe"));
        }
    }

    @Test
    @DisplayName("un SPEI generado cumple las reglas de su canal")
    void speiCumpleSuCanal() {
        for (int i = 0; i < SAMPLES; i++) {
            Map<String, String> d = SampleDeposit.spei();
            assertTrue(FundingDeposit.SPEI.equals(d.get("canal")));
            assertNull(d.get("sucursal"), "un SPEI no tiene sucursal");
            assertRellenos(d, "referencia", "ordenanteNombre", "institucion",
                              "monto", "fechaOperacion", "ordenanteClabe");
        }
    }

    @Test
    @DisplayName("un depósito en ventanilla cumple las reglas de su canal")
    void ventanillaCumpleSuCanal() {
        for (int i = 0; i < SAMPLES; i++) {
            Map<String, String> d = SampleDeposit.ventanilla();
            assertTrue(FundingDeposit.VENTANILLA.equals(d.get("canal")));
            assertNull(d.get("ordenanteClabe"), "en efectivo no hay cuenta ordenante");
            // El RFC es lo que compensa la falta de cuenta ordenante, así que
            // aquí no puede faltar: sin él el servicio rechaza el depósito.
            assertRellenos(d, "referencia", "ordenanteNombre", "ordenanteRfc",
                              "institucion", "sucursal", "monto", "fechaOperacion");
        }
    }

    @Test
    @DisplayName("la fecha generada nunca está en el futuro")
    void fechaSiempreEnPasado() {
        // El servicio rechaza un depósito con fecha futura; generar una sería
        // fabricar un caso que no se puede guardar.
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 0; i < SAMPLES; i++) {
            for (Map<String, String> d : java.util.List.of(
                    SampleDeposit.spei(), SampleDeposit.ventanilla())) {
                LocalDateTime fecha = LocalDateTime.parse(d.get("fechaOperacion"));
                assertFalse(fecha.isAfter(ahora), "fecha en el futuro: " + fecha);
            }
        }
    }

    @Test
    @DisplayName("el monto generado es una cantidad positiva")
    void montoPositivo() {
        for (int i = 0; i < SAMPLES; i++) {
            BigDecimal monto = new BigDecimal(SampleDeposit.spei().get("monto"));
            assertTrue(monto.signum() > 0, "monto no positivo: " + monto);
        }
    }

    @Test
    @DisplayName("el RFC generado tiene la forma de un RFC de persona moral")
    void rfcConForma() {
        // Tres letras, seis dígitos de fecha y tres de homoclave. No es un RFC
        // verificable —la homoclave real la calcula el SAT— pero sí bien formado.
        for (int i = 0; i < SAMPLES; i++) {
            String rfc = SampleDeposit.spei().get("ordenanteRfc");
            assertTrue(rfc.matches("[A-Z]{3}[0-9]{6}[A-Z0-9]{3}"), "RFC mal formado: " + rfc);
        }
    }

    @Test
    @DisplayName("dos referencias seguidas no salen iguales")
    void referenciasVariadas() {
        /*
          La unicidad de verdad la garantiza el UNIQUE de la base, no esto. Pero
          un generador que repitiera referencia haría que el segundo "generar y
          publicar" diera 409, y parecería un fallo del sistema cuando sería del
          generador. No se exige unicidad perfecta: sólo que no colisione de
          forma sistemática.
         */
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < SAMPLES; i++) vistas.add(SampleDeposit.spei().get("referencia"));
        assertTrue(vistas.size() > SAMPLES * 0.9,
                "demasiadas referencias repetidas: " + vistas.size() + " de " + SAMPLES);
    }

    private void assertRellenos(Map<String, String> d, String... campos) {
        for (String campo : campos) {
            String value = d.get(campo);
            assertTrue(value != null && !value.isBlank(), "falta " + campo + " en " + d);
        }
    }
}
