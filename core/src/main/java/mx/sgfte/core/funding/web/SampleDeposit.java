package mx.sgfte.core.funding.web;

import mx.sgfte.core.funding.Clabe;
import mx.sgfte.core.funding.FundingDeposit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Genera un depósito de prueba con datos plausibles.
 *
 * Existe por comodidad y no por diseño: teclear a mano una CLABE de 18 dígitos
 * con su dígito de control correcto, y encima inventarse una clave de rastreo
 * distinta cada vez, hace que probar el módulo sea más lento que construirlo.
 *
 * Vive en el paquete del simulador, junto a la pantalla que desaparece al
 * desplegar de verdad. No es parte del dominio: nada del sistema depende de
 * esta clase.
 *
 * <p><b>La CLABE se arma con Clabe.checkDigit()</b>, la misma implementación
 * que después la valida. Es a propósito: si el generador tuviera su propia
 * copia del algoritmo —o peor, una en JavaScript en la pantalla— las dos
 * podrían separarse, y el resultado sería un generador que produce CLABEs que
 * el validador rechaza. Así, por construcción, lo que se genera pasa.
 */
final class SampleDeposit {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /**
     * Instituciones reales con su código de tres dígitos, que es el que abre la
     * CLABE. Van emparejados para que el nombre del banco y el prefijo de la
     * cuenta no se contradigan: una CLABE que empieza en 012 y dice "BANORTE"
     * se nota a la primera y estropea la demo.
     */
    private static final String[][] BANKS = {
            { "002", "BANAMEX" },
            { "012", "BBVA MEXICO" },
            { "014", "SANTANDER" },
            { "021", "HSBC" },
            { "030", "BANBAJIO" },
            { "036", "INBURSA" },
            { "044", "SCOTIABANK" },
            { "058", "BANREGIO" },
            { "062", "AFIRME" },
            { "072", "BANORTE" },
            { "127", "AZTECA" },
            { "646", "STP" },
    };

    private static final String[] GIROS = {
            "COMERCIALIZADORA", "DISTRIBUIDORA", "CORPORATIVO", "GRUPO INDUSTRIAL",
            "SERVICIOS INTEGRALES", "CONSTRUCTORA", "TRANSPORTES", "MANUFACTURAS",
            "PROVEEDORA", "OPERADORA",
    };

    private static final String[] APELLIDOS = {
            "DEL BAJIO", "DEL SUR", "MORELOS", "DEL VALLE", "PENINSULAR",
            "DEL NORTE", "AZTECA", "CUAUHNAHUAC", "TEPEYAC", "ANAHUAC",
    };

    private static final String[] SUCURSALES = {
            "Cuernavaca Centro", "Cuernavaca Galerías", "Jiutepec Civac",
            "Temixco Acatlipa", "Cuautla Plaza", "Emiliano Zapata",
    };

    private static final String[] CONCEPTOS_SPEI = {
            "Fondeo nomina", "Fondeo operativo", "Fondeo viaticos",
            "Aportacion mensual", "Fondeo combustible",
    };

    private static final String[] CONCEPTOS_CASH = {
            "Deposito en efectivo", "Aportacion en ventanilla", "Fondeo en sucursal",
    };

    private SampleDeposit() {}

    /** Un SPEI completo: trae cuenta ordenante y clave de rastreo. */
    static Map<String, String> spei() {
        String[] bank = pick(BANKS);
        Map<String, String> d = common(FundingDeposit.SPEI, bank[1]);
        d.put("ordenanteClabe", clabe(bank[0]));
        d.put("referencia", claveDeRastreo(bank[1]));
        d.put("referenciaNumerica", String.valueOf(rnd(1000000, 9999999)));
        d.put("concepto", pick(CONCEPTOS_SPEI) + " " + mes());
        return d;
    }

    /** Un depósito en ventanilla: sin cuenta ordenante, con folio y sucursal. */
    static Map<String, String> ventanilla() {
        String[] bank = pick(BANKS);
        Map<String, String> d = common(FundingDeposit.VENTANILLA, bank[1]);
        d.put("sucursal", pick(SUCURSALES));
        d.put("referencia", folio());
        d.put("concepto", pick(CONCEPTOS_CASH));
        return d;
    }

    private static Map<String, String> common(String canal, String institucion) {
        String razonSocial = razonSocial();
        Map<String, String> d = new LinkedHashMap<>();
        d.put("canal", canal);
        d.put("ordenanteNombre", razonSocial);
        d.put("ordenanteRfc", rfc(razonSocial));
        d.put("institucion", institucion);
        d.put("monto", monto());
        // Hacia atrás en el tiempo, nunca hacia delante: el servicio rechaza un
        // depósito con fecha futura, así que un generador que la produjera
        // estaría fabricando casos que no se pueden guardar.
        d.put("fechaOperacion", LOCAL.format(LocalDateTime.now().minusMinutes(rnd(5, 2880))));
        return d;
    }

    /**
     * CLABE válida: 3 de institución + 3 de plaza + 11 de cuenta + el dígito de
     * control que calcula Clabe.
     *
     * La plaza va aleatoria y no se pretende que corresponda a una ciudad real:
     * el dígito de control no la verifica y nada en el sistema la interpreta.
     * Lo que sí tiene que cuadrar es el dígito, y de eso se encarga Clabe.
     */
    private static String clabe(String bankCode) {
        StringBuilder first17 = new StringBuilder(bankCode);
        first17.append(String.format("%03d", rnd(1, 999)));          // plaza
        for (int i = 0; i < 11; i++) first17.append(rnd(0, 9));      // cuenta
        return first17.toString() + Clabe.checkDigit(first17.toString());
    }

    /**
     * Clave de rastreo con pinta de real.
     *
     * El formato exacto lo decide cada institución —no hay uno único—, así que
     * ésta es representativa y no canónica: prefijo del banco, fecha y un
     * consecutivo. Lo que sí importa para el sistema es que no se repita, y de
     * eso responde el UNIQUE de la base, no este generador.
     */
    private static String claveDeRastreo(String institucion) {
        String tag = institucion.replaceAll("[^A-Z]", "");
        tag = (tag + "XXXX").substring(0, 4);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return tag + "01" + fecha + String.format("%010d", rnd(1, 999999999));
    }

    private static String folio() {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MMdd"));
        return "FICHA-" + fecha + "-" + String.format("%05d", rnd(1, 99999));
    }

    private static String razonSocial() {
        return pick(GIROS) + " " + pick(APELLIDOS) + " SA DE CV";
    }

    /**
     * RFC de persona moral con la FORMA correcta: tres letras, seis dígitos de
     * fecha y tres de homoclave.
     *
     * La homoclave real la calcula el SAT con su propio algoritmo, así que esto
     * no es un RFC verificable — es un dato de prueba bien formado. El sistema
     * tampoco lo valida más allá de exigirlo en ventanilla; si algún día se
     * validara de verdad, este generador tendría que cambiar.
     */
    private static String rfc(String razonSocial) {
        StringBuilder letras = new StringBuilder();
        for (String palabra : razonSocial.split(" ")) {
            if (palabra.length() > 2 && letras.length() < 3) letras.append(palabra.charAt(0));
        }
        while (letras.length() < 3) letras.append('X');

        // El año va de un rango de cuatro cifras y luego se recorta a dos. Ir
        // directo a dos cifras invita a escribir rnd(85, 24) pensando en
        // "de 1985 a 2024", que no es un rango: es un límite inferior mayor que
        // el superior, y revienta al generar.
        int anio = rnd(1985, 2024) % 100;
        String fecha = String.format("%02d%02d%02d", anio, rnd(1, 12), rnd(1, 28));
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder homoclave = new StringBuilder();
        for (int i = 0; i < 3; i++) homoclave.append(alfabeto.charAt(rnd(0, alfabeto.length() - 1)));
        return letras + fecha + homoclave.toString();
    }

    /** Montos con pinta de fondeo empresarial, con centavos para que no salgan redondos. */
    private static String monto() {
        return rnd(25, 900) + "" + String.format("%03d.%02d", rnd(0, 999), rnd(0, 99));
    }

    private static String mes() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM", java.util.Locale.forLanguageTag("es-MX")));
    }

    private static <T> T pick(T[] options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    private static int rnd(int from, int toInclusive) {
        // El mensaje de ThreadLocalRandom para un rango invertido es "bound must
        // be greater than origin", que no dice cuál de las llamadas fue. Con
        // veinte en esta clase, decirlo aquí ahorra el rato de buscarla.
        if (from > toInclusive) {
            throw new IllegalArgumentException("Rango invertido: " + from + ".." + toInclusive);
        }
        return ThreadLocalRandom.current().nextInt(from, toInclusive + 1);
    }
}
