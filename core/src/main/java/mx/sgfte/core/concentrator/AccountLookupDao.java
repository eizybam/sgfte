package mx.sgfte.core.concentrator;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read-only lookups of active accounts for the dispersion screen. */
public class AccountLookupDao {

    public List<AccountOption> findActiveForSelect() {
        String sql = "SELECT a.id, a.balance, c.first_name, c.last_name, cat.name AS purpose "
                + "FROM account a "
                + "JOIN cardholder c ON c.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id "
                + "WHERE a.status = 'ACTIVE' "
                + "ORDER BY c.last_name, cat.name";
        List<AccountOption> options = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString("last_name") + ", " + rs.getString("first_name")
                        + " — " + rs.getString("purpose");
                options.add(new AccountOption(rs.getLong("id"), label, rs.getBigDecimal("balance")));
            }
            return options;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts for dispersion", e);
        }
    }

    /**
     * La etiqueta de UNA cuenta: "López, Ana — Gasolina".
     *
     * Existe porque DispersionServlet.labelOf() cargaba findActiveForSelect()
     * entera y la recorría con un stream para quedarse con un solo campo — una
     * tabla completa en memoria para leer una cadena. Con el selector paginado
     * eso ya no tiene ni la excusa de que la lista estuviera cargada de todas
     * formas.
     *
     * Optional porque la cuenta puede haberse desactivado entre que se eligió y
     * que se consultó; quien llama decide qué enseñar entonces.
     */
    public Optional<String> findLabel(long accountId) {
        String sql = "SELECT c.first_name, c.last_name, cat.name AS purpose "
                + "FROM account a "
                + "JOIN cardholder c ON c.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id "
                + "WHERE a.id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rs.getString("last_name") + ", " + rs.getString("first_name")
                        + " — " + rs.getString("purpose"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading account label " + accountId, e);
        }
    }

}
