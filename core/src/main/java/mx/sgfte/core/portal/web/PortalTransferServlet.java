package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.AccountNotOwnedException;
import mx.sgfte.core.portal.PortalService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * P2P transfer for the employee (HU-07, Figma: "Transferencia").
 *
 * The source dropdown lists only the employee's OWN accounts; the destination
 * dropdown fills in once a source is chosen, with colleagues' accounts of the
 * same purpose (RN-07). Picking a source re-submits the form with GET, the same
 * pattern the admin historial screen uses, so no JavaScript is needed.
 *
 * Note this is the same TransferService the admin screen calls. The rule about
 * matching purposes is not re-implemented here — this servlet only adds "the
 * source must be mine".
 */
@WebServlet("/app/transferencia")
public class PortalTransferServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp, PortalSupport.parseId(req.getParameter("sourceId")));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long sourceId = PortalSupport.parseId(req.getParameter("sourceId"));
        Long destId = PortalSupport.parseId(req.getParameter("destId"));
        BigDecimal amount = PortalSupport.parseAmount(req.getParameter("amount"));

        try {
            portalService.transfer(cardholderId, sourceId, destId, amount,
                    req.getParameter("description"));
            req.setAttribute("success", "Transferencia realizada.");
            // Clear the source so the form comes back blank after a success and
            // a page refresh can't repeat the amount by accident.
            sourceId = null;
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        } catch (AccountNotOwnedException e) {
            // Tampered sourceId. Say nothing specific about it.
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        render(req, resp, sourceId);
    }

    /** Loads both dropdowns and shows the form. */
    private void render(HttpServletRequest req, HttpServletResponse resp, Long sourceId)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);

        req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));
        req.setAttribute("selectedSourceId", sourceId);
        if (sourceId != null) {
            List<?> peers = portalService.peersFor(cardholderId, sourceId);
            req.setAttribute("peers", peers);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/app/transferencia.jsp").forward(req, resp);
    }
}
