package mx.sgfte.core.funding;

/**
 * CLABE: Clave Bancaria Estandarizada, los 18 dígitos que identifican una
 * cuenta en México.
 *
 * <p>Estructura: 3 dígitos de institución + 3 de plaza + 11 de cuenta + 1 de
 * control. El último NO es un dígito más: se calcula a partir de los otros 17,
 * así que una CLABE mal tecleada se detecta sin preguntarle a nadie.
 *
 * <p>Por qué está aquí y no como una validación de longitud en el servlet: la
 * CLABE es de dónde viene el dinero, y en este sistema el origen es el dato que
 * da sentido al asiento. Aceptar una CLABE imposible sería registrar una
 * procedencia que no existe, que es exactamente lo que V12 vino a impedir. El
 * dígito de control convierte "confío en quien capturó" en "la aritmética no
 * cuadra", que es una garantía distinta.
 *
 * <p>El algoritmo: cada uno de los primeros 17 dígitos se multiplica por su
 * ponderador (3, 7, 1 repetidos) y se toma SÓLO la última cifra del producto —
 * ese detalle es el que se salta todo el mundo al implementarlo—. Se suman las
 * 17 cifras, se saca el residuo entre 10 y el dígito de control es (10 −
 * residuo) mod 10.
 */
public final class Clabe {

    public static final int LENGTH = 18;

    /** 3, 7, 1 repetidos; se aplican a los primeros 17 dígitos. */
    private static final int[] WEIGHTS = { 3, 7, 1 };

    private Clabe() {}

    /**
     * ¿Es una CLABE bien formada y con dígito de control correcto?
     *
     * <p>Null, espacios, longitud distinta de 18 o cualquier carácter que no
     * sea dígito devuelven false. No lanza: quien llama decide qué mensaje
     * enseñar.
     */
    public static boolean isValid(String clabe) {
        String digits = normalize(clabe);
        if (digits == null) return false;
        /*
          Dieciocho ceros CUADRAN: la suma ponderada da 0 y el dígito de control
          de 0 es 0. Es la CLABE que alguien teclea cuando quiere rellenar el
          campo y seguir, así que el checksum solo la dejaría pasar. La
          institución 000 no está asignada en el catálogo de Banxico —empieza en
          002— de modo que rechazarla no descarta ninguna cuenta real.
         */
        if (digits.startsWith("000")) return false;
        int expected = checkDigit(digits.substring(0, LENGTH - 1));
        return expected == (digits.charAt(LENGTH - 1) - '0');
    }

    /**
     * El dígito de control que le corresponde a los 17 primeros.
     *
     * <p>Público porque el simulador de banco lo necesita para poder generar
     * CLABEs válidas: sin esto, dar de alta un depósito de prueba obligaría a
     * calcular el dígito a mano.
     *
     * @throws IllegalArgumentException si no son exactamente 17 dígitos.
     */
    public static int checkDigit(String first17) {
        if (first17 == null || first17.length() != LENGTH - 1) {
            throw new IllegalArgumentException("Se esperaban 17 dígitos, llegaron: " + first17);
        }
        int sum = 0;
        for (int i = 0; i < first17.length(); i++) {
            char c = first17.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("La CLABE sólo admite dígitos");
            }
            // Sólo la última cifra del producto entra en la suma.
            sum += ((c - '0') * WEIGHTS[i % WEIGHTS.length]) % 10;
        }
        return (10 - (sum % 10)) % 10;
    }

    /** Los tres primeros dígitos: la institución que lleva la cuenta. */
    public static String bankCode(String clabe) {
        String digits = normalize(clabe);
        return digits == null ? null : digits.substring(0, 3);
    }

    /**
     * Para enseñarla sin escupir 18 dígitos seguidos: 012 180 00000000000 2.
     * Devuelve la entrada tal cual si no es una CLABE con forma de CLABE.
     */
    public static String format(String clabe) {
        String digits = normalize(clabe);
        if (digits == null) return clabe;
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " "
             + digits.substring(6, 17) + " " + digits.substring(17);
    }

    /**
     * Quita espacios y guiones —se copian y pegan desde el comprobante del
     * banco, que los trae— y verifica forma. Devuelve null si no sirve.
     */
    public static String normalize(String clabe) {
        if (clabe == null) return null;
        String digits = clabe.replaceAll("[\\s-]", "");
        if (digits.length() != LENGTH) return null;
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        return digits;
    }
}
