package mx.sgfte.core.funding;

import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las reglas del depósito, sin base de datos.
 *
 * Todos estos casos se rechazan ANTES de abrir conexión, así que el DAO nunca
 * entra en juego: por eso las dependencias van en null. Si alguna prueba
 * empieza a fallar con NullPointerException es la señal de que una validación
 * dejó de rechazar lo que debía y la ejecución llegó hasta la transacción.
 *
 * Las mismas reglas de canal viven además como CHECK constraints en V12. Se
 * comprueban aquí para poder dar el mensaje en español, y allá para que ninguna
 * otra ruta se las salte.
 */
class FundingServiceValidationTest {

    private static final String NUESTRA   = "012180000000000002";
    private static final String ORDENANTE = "002180000123456789";

    private final FundingService service = new FundingService(null, null);

    private FundingDeposit spei() {
        FundingDeposit d = new FundingDeposit();
        d.setCanal(FundingDeposit.SPEI);
        d.setReferencia("MBAN01002608180001234567");
        d.setOrdenanteNombre("COMERCIALIZADORA ACME SA DE CV");
        d.setOrdenanteClabe(ORDENANTE);
        d.setInstitucion("BANAMEX");
        d.setBeneficiarioClabe(NUESTRA);
        d.setMonto(new BigDecimal("150000.00"));
        d.setFechaOperacion(LocalDateTime.now().minusMinutes(10));
        return d;
    }

    private FundingDeposit ventanilla() {
        FundingDeposit d = new FundingDeposit();
        d.setCanal(FundingDeposit.VENTANILLA);
        d.setReferencia("FOLIO-99887766");
        d.setOrdenanteNombre("COMERCIALIZADORA ACME SA DE CV");
        d.setOrdenanteRfc("CAC010101AB1");
        d.setInstitucion("BBVA");
        d.setSucursal("Cuernavaca Centro");
        d.setBeneficiarioClabe(NUESTRA);
        d.setMonto(new BigDecimal("40000.00"));
        d.setFechaOperacion(LocalDateTime.now().minusHours(2));
        return d;
    }

    private ValidationException rejected(FundingDeposit d) {
        return assertThrows(ValidationException.class, () -> service.register(d, NUESTRA));
    }

    private void assertComplains(FundingDeposit d, String fragment) {
        ValidationException e = rejected(d);
        assertTrue(e.getErrors().stream().anyMatch(m -> m.toLowerCase().contains(fragment.toLowerCase())),
                "se esperaba un error sobre '" + fragment + "' y llegaron: " + e.getErrors());
    }

    // ---- Lo que sí debe pasar la validación --------------------------------

    @Test
    @DisplayName("un SPEI y un depósito en ventanilla bien formados pasan la validación")
    void losValidosPasanValidacion() {
        // Sin base no pueden completarse, pero tienen que morir en la conexión,
        // no en las reglas: cualquier cosa MENOS ValidationException.
        for (FundingDeposit d : new FundingDeposit[] { spei(), ventanilla() }) {
            Exception e = assertThrows(Exception.class, () -> service.register(d, NUESTRA));
            assertEquals(false, e instanceof ValidationException,
                    "un depósito válido no debería ser rechazado por las reglas: " + e.getMessage());
        }
    }

    // ---- La asimetría entre canales, que es el corazón del diseño ----------

    @Test
    @DisplayName("SPEI sin CLABE ordenante se rechaza: una transferencia siempre viene de una cuenta")
    void speiExigeOrdenante() {
        FundingDeposit d = spei();
        d.setOrdenanteClabe(null);
        assertComplains(d, "CLABE ordenante");
    }

    @Test
    @DisplayName("SPEI con CLABE ordenante inválida se rechaza por el dígito de control")
    void speiExigeOrdenanteReal() {
        FundingDeposit d = spei();
        d.setOrdenanteClabe("002180000123456780");   // mismo prefijo, dígito cambiado
        assertComplains(d, "CLABE ordenante");
    }

    @Test
    @DisplayName("ventanilla sin RFC se rechaza: sin cuenta ordenante es lo único que identifica")
    void ventanillaExigeRfc() {
        FundingDeposit d = ventanilla();
        d.setOrdenanteRfc(null);
        assertComplains(d, "RFC");
    }

    @Test
    @DisplayName("ventanilla con CLABE ordenante se rechaza: en efectivo no hay cuenta que ordene")
    void ventanillaNoLlevaOrdenante() {
        FundingDeposit d = ventanilla();
        d.setOrdenanteClabe(ORDENANTE);
        assertComplains(d, "no tiene CLABE ordenante");
    }

    @Test
    @DisplayName("ventanilla sin sucursal se rechaza")
    void ventanillaExigeSucursal() {
        FundingDeposit d = ventanilla();
        d.setSucursal(null);
        assertComplains(d, "sucursal");
    }

    @Test
    @DisplayName("un SPEI con sucursal se rechaza: nadie se paró en ningún mostrador")
    void speiNoLlevaSucursal() {
        FundingDeposit d = spei();
        d.setSucursal("Cuernavaca Centro");
        assertComplains(d, "sucursal");
    }

    // ---- El destino --------------------------------------------------------

    @Test
    @DisplayName("un depósito dirigido a otra CLABE no es dinero nuestro")
    void beneficiarioTieneQueSerNuestraClabe() {
        FundingDeposit d = spei();
        d.setBeneficiarioClabe("072180000987654320");
        assertComplains(d, "Concentradora");
    }

    @Test
    @DisplayName("una CLABE beneficiaria imposible se rechaza")
    void beneficiarioTieneQueSerValida() {
        FundingDeposit d = spei();
        d.setBeneficiarioClabe("012180000000000009");   // dígito de control mal
        assertComplains(d, "beneficiaria");
    }

    // ---- Lo obligatorio en los dos canales ---------------------------------

    @Test
    @DisplayName("sin referencia bancaria no hay depósito que registrar")
    void referenciaObligatoria() {
        FundingDeposit d = spei();
        d.setReferencia("   ");
        assertComplains(d, "referencia");
    }

    @Test
    @DisplayName("sin razón social no se registra: un depósito tiene que tener depositante")
    void razonSocialObligatoria() {
        FundingDeposit d = spei();
        d.setOrdenanteNombre(null);
        assertComplains(d, "razón social");
    }

    @Test
    @DisplayName("el monto tiene que ser positivo")
    void montoPositivo() {
        FundingDeposit cero = spei();
        cero.setMonto(BigDecimal.ZERO);
        assertComplains(cero, "monto");

        FundingDeposit negativo = spei();
        negativo.setMonto(new BigDecimal("-500"));
        assertComplains(negativo, "monto");
    }

    @Test
    @DisplayName("un depósito del futuro no ha ocurrido")
    void fechaNoPuedeSerFutura() {
        FundingDeposit d = spei();
        d.setFechaOperacion(LocalDateTime.now().plusDays(1));
        assertComplains(d, "futuro");
    }

    @Test
    @DisplayName("un canal que no existe se rechaza en vez de tratarse como ventanilla")
    void canalDesconocido() {
        FundingDeposit d = spei();
        d.setCanal("PAYPAL");
        assertComplains(d, "Canal");
    }

    @Test
    @DisplayName("se acumulan todos los errores, no sólo el primero")
    void reportaTodosLosErrores() {
        FundingDeposit d = spei();
        d.setReferencia(null);
        d.setOrdenanteNombre(null);
        d.setMonto(null);
        ValidationException e = rejected(d);
        assertTrue(e.getErrors().size() >= 3,
                "se esperaban al menos 3 errores y llegaron " + e.getErrors());
    }
}
