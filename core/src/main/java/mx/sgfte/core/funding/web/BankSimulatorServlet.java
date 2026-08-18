package mx.sgfte.core.funding.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.concentrator.ConcentratorDao;
import mx.sgfte.core.funding.FundingConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET/POST /admin/simulador-banco — el banco, de mentiras.
 *
 * ESTA PANTALLA NO ES PARTE DEL SISTEMA. Ocupa el lugar del banco: es donde se
 * captura lo que en producción llegaría solo, cuando el PSP avisa que entró una
 * transferencia. En un despliegue real se borra el JSP y este servlet, el
 * webhook del banco pega contra /api/banco/deposito y NO CAMBIA NADA MÁS: ni el
 * servicio, ni el ledger, ni las reglas.
 *
 * Es importante que se vea que es un simulador y no disimularlo. Lo honesto de
 * la defensa no es "es imposible inventarse un depósito" —con el simulador a la
 * mano, claro que se puede— sino que el sistema sólo acepta dinero como un
 * hecho externo con referencia verificable, y que cambiar el simulador por un
 * banco de verdad no obliga a tocar el núcleo.
 *
 * Por eso publica de verdad contra el endpoint, firmando con HMAC, en vez de
 * llamar a FundingService directamente: así el camino que se prueba en la demo
 * es exactamente el que se usará en producción, firma incluida.
 *
 * El secreto vive aquí, en el servidor, y nunca baja al navegador — si la
 * página firmara en JavaScript, el secreto estaría en el código fuente de la
 * pestaña y no protegería de nada.
 */
@WebServlet("/admin/simulador-banco")
public class BankSimulatorServlet extends HttpServlet {

    private static final String FLASH_OK     = "simOk";
    private static final String FLASH_ERROR  = "simError";
    private static final String FLASH_FORM   = "simForm";

    private final ConcentratorDao concentrator = new ConcentratorDao();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("concentrador", concentrator.findSingleton());
        req.setAttribute("secretoPorDefecto", FundingConfig.usingDefaultSecret());
        consumeFlash(req);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/simulador-banco.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();

        // Sólo se reenvía lo que el endpoint entiende: si la pantalla ganara un
        // campo suyo, iría a la firma y el receptor no podría reproducirla.
        Map<String, String> params = new LinkedHashMap<>();
        for (String field : DepositInboundServlet.FIELDS) {
            String value = req.getParameter(field);
            if (value != null && !value.isBlank()) params.put(field, value.trim());
        }

        try {
            HttpResponse<String> response = publish(req, params);
            if (response.statusCode() == 200) {
                session.setAttribute(FLASH_OK, describe(params));
            } else {
                session.setAttribute(FLASH_ERROR, reason(response));
                session.setAttribute(FLASH_FORM, params);
            }
        } catch (Exception e) {
            // El endpoint es local, así que esto sólo pasa si la app se está
            // reiniciando. Se dice, en vez de dejar la pantalla en blanco.
            session.setAttribute(FLASH_ERROR, "No se pudo contactar el endpoint: " + e.getMessage());
            session.setAttribute(FLASH_FORM, params);
        }

        // Post/redirect/get: refrescar no debe repetir un depósito. Que además
        // chocaría contra el UNIQUE, pero la pantalla no debería llegar a eso.
        resp.sendRedirect(req.getContextPath() + "/admin/simulador-banco");
    }

    /** Firma y publica, exactamente como lo haría el banco. */
    private HttpResponse<String> publish(HttpServletRequest req, Map<String, String> params)
            throws Exception {

        Map<String, String[]> forSigning = new LinkedHashMap<>();
        params.forEach((k, v) -> forSigning.put(k, new String[] { v }));
        String firma = DepositSignature.sign(
                DepositSignature.canonical(forSigning), FundingConfig.BANK_SECRET);

        List<String> encoded = new ArrayList<>();
        params.forEach((k, v) -> encoded.add(encode(k) + "=" + encode(v)));
        encoded.add(DepositSignature.PARAM + "=" + encode(firma));

        /*
          La URL se arma desde la propia petición y no desde SGFTE_APP_BASE_URL:
          esa apunta a donde ve la app el navegador, que dentro del contenedor
          no siempre resuelve. Esto siempre apunta a sí mismo.
         */
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                + req.getContextPath();

        HttpRequest post = HttpRequest.newBuilder(URI.create(base + "/api/banco/deposito"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.join("&", encoded), StandardCharsets.UTF_8))
                .build();

        return http.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String describe(Map<String, String> params) {
        return params.getOrDefault("canal", "") + " · "
             + params.getOrDefault("referencia", "") + " · $"
             + params.getOrDefault("monto", "");
    }

    /** Saca el motivo del JSON sin librería: es un campo y siempre viene igual. */
    private String reason(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        int at = body.indexOf("\"motivo\":\"");
        if (at < 0) return "HTTP " + response.statusCode();
        int from = at + "\"motivo\":\"".length();
        int to = body.indexOf('"', from);
        return to < 0 ? "HTTP " + response.statusCode() : body.substring(from, to);
    }

    @SuppressWarnings("unchecked")
    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        for (String key : List.of(FLASH_OK, FLASH_ERROR, FLASH_FORM)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        if (req.getAttribute(FLASH_FORM) == null) {
            req.setAttribute(FLASH_FORM, Collections.<String, String>emptyMap());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
