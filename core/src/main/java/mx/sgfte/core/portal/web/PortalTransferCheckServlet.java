package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.PeerOption;
import mx.sgfte.core.portal.PortalService;

import java.io.IOException;
import java.util.Optional;

/**
 * "¿Este identificador es una cuenta a la que puedo transferir?" — la respuesta
 * mientras el empleado teclea.
 *
 * Es SÓLO una cortesía de la pantalla. Quien decide sigue siendo
 * PortalTransferServlet, que resuelve el destino otra vez al enviar y con la
 * misma consulta: si alguien salta esta comprobación, o si la cuenta cambia
 * entre que se escribe y que se confirma, la transferencia se rechaza igual.
 *
 * Devuelve HTML y no JSON —un fragmento que la pantalla mete tal cual—, como el
 * selector con tabla del área de administración: el escapado lo hace JSTL y no
 * hay que construir nodos a mano en el navegador.
 *
 * Lo que revela está medido: el nombre del compañero SÓLO cuando el destino ya
 * es válido, que es cuando hace falta para no equivocarse de persona. Cualquier
 * otro caso —no existe, inactiva, tuya, de otro propósito— da el MISMO mensaje.
 * Distinguirlos convertiría este campo en un buscador de cuentas ajenas, y el
 * identificador es justo lo que no se puede adivinar.
 */
@WebServlet("/app/transferencia/validar")
public class PortalTransferCheckServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-store");

        long cardholderId = PortalSupport.cardholderId(req);
        Long sourceId = PortalSupport.parseId(req.getParameter("sourceId"));
        String typed = req.getParameter("dest");

        Optional<PeerOption> peer = portalService.peerByNumber(cardholderId, sourceId, typed);

        req.setAttribute("peer", peer.orElse(null));
        req.getRequestDispatcher("/WEB-INF/jsp/app/transfer-check.jsp").forward(req, resp);
    }
}
