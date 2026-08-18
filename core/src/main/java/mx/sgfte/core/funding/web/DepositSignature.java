package mx.sgfte.core.funding.web;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Firma HMAC-SHA256 de la notificación bancaria.
 *
 * El endpoint de depósitos no puede ir detrás de AuthFilter: no lo llama una
 * persona con sesión, lo llama un sistema. Lo que lo protege es que quien
 * publica tiene que conocer el secreto compartido, igual que en el webhook de
 * un PSP real.
 *
 * <p>La cadena que se firma se arma ordenando los parámetros por nombre y
 * uniéndolos como {@code clave=valor&clave=valor}. Ordenar importa: si se
 * firmara en el orden en que llegan, la misma petición daría firmas distintas
 * según cómo la serialice el cliente, y el receptor no podría reproducirla.
 *
 * <p>No lleva marca de tiempo ni nonce, y es una decisión, no un olvido:
 * reenviar una notificación idéntica no consigue nada porque la referencia del
 * depósito es UNIQUE en la base, así que el segundo intento choca contra el
 * índice y se responde 409. La protección contra repetición ya la da el modelo
 * de datos.
 */
public final class DepositSignature {

    private static final String ALGORITHM = "HmacSHA256";

    /** El parámetro donde viaja la firma; no forma parte de lo firmado. */
    public static final String PARAM = "firma";

    private DepositSignature() {}

    /** La cadena canónica de un mapa de parámetros, sin la propia firma. */
    public static String canonical(Map<String, String[]> params) {
        List<String> names = new ArrayList<>(params.keySet());
        names.remove(PARAM);
        Collections.sort(names);

        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            String[] values = params.get(name);
            String value = (values == null || values.length == 0) ? "" : values[0];
            if (sb.length() > 0) sb.append('&');
            sb.append(name).append('=').append(value == null ? "" : value);
        }
        return sb.toString();
    }

    public static String sign(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar la notificación", e);
        }
    }

    /**
     * ¿La petición viene firmada con el secreto que compartimos?
     *
     * La comparación es MessageDigest.isEqual y no String.equals: la de String
     * corta en el primer byte distinto, y ese tiempo distinto es una vía para
     * ir adivinando la firma byte a byte. Aquí no es un riesgo realista, pero
     * comparar firmas en tiempo constante cuesta lo mismo que no hacerlo.
     */
    public static boolean verify(HttpServletRequest req, String secret) {
        String provided = req.getParameter(PARAM);
        if (provided == null || provided.isBlank()) return false;
        String expected = sign(canonical(req.getParameterMap()), secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
