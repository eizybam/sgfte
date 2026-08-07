package mx.sgfte.core.transfers.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.transfers.TransferService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * P2P transfer screen. Placed under /admin/* for now so AuthFilter protects it;
 * in the final flow this moves to the employee area /app/* once Module 6 adds
 * the employee filter. Reuses M1's AccountLookupDao for the dropdowns.
 */
@WebServlet("/admin/transferencia")
public class TransferServlet extends HttpServlet {

    private final TransferService transferService = new TransferService();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long sourceId = parseId(req.getParameter("sourceId"));
        Long destId = parseId(req.getParameter("destId"));
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        // PRG y no forward: una transferencia mueve dinero, y recargar no debe
        // repetirla. Antes se reenviaba, así que F5 la volvía a intentar.
        try {
            transferService.transfer(sourceId, destId, amount, req.getParameter("description"));

            mx.sgfte.core.shared.web.OperationResult.success("¡Transferencia realizada!",
                            "El saldo se movió entre cuentas",
                            "TRANSFERENCIA CONFIRMADA",
                            "Ambas cuentas comparten propósito, que es la condición para transferir.")
                    .amount("Monto transferido", amount)
                    .detail("Origen", labelOf(sourceId))
                    .detail("Destino", labelOf(destId))
                    .when(java.time.LocalDateTime.now())
                    .primary("Ver cuenta destino", "/admin/cuenta?id=" + destId)
                    .flash(req.getSession());
        } catch (ValidationException e) {
            mx.sgfte.core.shared.web.OperationResult.rejected("Transferencia rechazada",
                            "La operación no pudo completarse",
                            String.join(" ", e.getErrors()))
                    .amount("Monto solicitado", amount)
                    .detail("Origen", labelOf(sourceId))
                    .detail("Destino", labelOf(destId))
                    .when(java.time.LocalDateTime.now())
                    .primary("Reintentar", "/admin/transferencia")
                    .flash(req.getSession());
        }
        resp.sendRedirect(req.getContextPath() + "/admin/transferencia");
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/transferencia.jsp").forward(req, resp);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return new BigDecimal(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** Cómo se lee una cuenta en la tarjeta de resultado. */
    private String labelOf(Long accountId) {
        if (accountId == null) return null;
        try {
            return accountLookupDao.findActiveForSelect().stream()
                    .filter(a -> a.getId() == accountId)
                    .map(a -> a.getLabel())
                    .findFirst()
                    .orElse("Cuenta " + accountId);
        } catch (RuntimeException e) {
            return "Cuenta " + accountId;
        }
    }
}
