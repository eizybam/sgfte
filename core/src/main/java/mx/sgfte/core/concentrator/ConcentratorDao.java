package mx.sgfte.core.concentrator;

import mx.sgfte.core.shared.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access for the Concentrator (singleton row: singleton = 'Y').
 * Methods that take a Connection join the Service's transaction (they do NOT
 * open their own). Methods without it are standalone read/updates.
 */
public class ConcentratorDao {

    /** Reads the Concentrator (read-only, own connection). */
    public ConcentratorAccount findSingleton() {
        String sql = "SELECT id, name, balance, clabe FROM concentrator_account WHERE singleton = 'Y'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ConcentratorAccount ca = new ConcentratorAccount();
                ca.setId(rs.getLong("id"));
                ca.setName(rs.getString("name"));
                ca.setBalance(rs.getBigDecimal("balance"));
                // CHAR(18): Oracle la devuelve rellenada a lo ancho de la columna.
                String clabe = rs.getString("clabe");
                ca.setClabe(clabe == null ? null : clabe.trim());
                return ca;
            }
            throw new IllegalStateException("Concentrator account not found (check the schema seed)");
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator", e);
        }
    }

    /*
      Aquí vivía fund(monto, actor), que abría su propia transacción, subía el
      saldo y escribía el asiento. Era el único camino por el que entraba
      dinero sin respaldo bancario, así que se eliminó con V12 en vez de
      dejarlo "por si acaso": un método que crea dinero y no tiene llamadores
      es una puerta esperando a que alguien la use.

      Su sustituto es creditFromDeposit(), abajo, que exige el id de un
      funding_deposit ya insertado y se suma a la transacción de quien llama.
     */

    /**
     * Debits the Concentrator inside the Service's transaction.
     * The "balance >= ?" guard prevents going negative: with no funds it affects
     * 0 rows and we return false so the Service can roll back.
     */
    public boolean debit(Connection conn, BigDecimal amount) throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance - ? "
                + "WHERE singleton = 'Y' AND balance >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            if (ps.executeUpdate() != 1) return false;   // no había saldo
        }
        record(conn, "DISPERSION", amount, null, null);
        return true;
    }

    /** Credits the Concentrator inside a transaction (used by reintegration, M4). */
    public void credit(Connection conn, BigDecimal amount) throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.executeUpdate();
        }
        record(conn, "REINTEGRATION", amount, null, null);
    }

    /**
     * Credits the Concentrator from a bank deposit, inside FundingService's
     * transaction (V12).
     *
     * Distinta de fund(): aquélla abría su propia transacción porque el fondeo
     * era un acto suelto de un administrador. Ahora el abono es una de las tres
     * escrituras de un mismo hecho —depósito, saldo y asiento—, así que se
     * suma a la transacción de quien llama en vez de abrir la suya.
     *
     * El asiento nace apuntando a su depósito. No hay forma de escribir un
     * FUNDING con respaldo "más tarde": o entra con él, o no entra.
     */
    public void creditFromDeposit(Connection conn, BigDecimal amount, String actor, long depositId)
            throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("No se pudo abonar a la Concentradora");
            }
        }
        record(conn, "FUNDING", amount, actor, depositId);
    }

    /**
     * Writes the ledger entry for a balance change.
     *
     * It lives here, next to the three methods that touch the balance, so that
     * no future caller can move Concentrator money without leaving a record —
     * putting it in the services would have left that up to whoever writes the
     * next one. Always on the caller's connection, so the entry commits with the
     * money or not at all.
     *
     * balance_after is read back inside the same transaction rather than
     * computed, so it is the real post-condition and not an assumption.
     */
    private void record(Connection conn, String type, BigDecimal amount, String actor,
                        Long depositId) throws SQLException {

        BigDecimal balanceAfter;
        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT balance FROM concentrator_account WHERE singleton = 'Y'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("Concentrator account not found");
            balanceAfter = rs.getBigDecimal(1);
        }

        String sql = "INSERT INTO concentrator_movement "
                   + "(movement_type, amount, balance_after, actor, funding_deposit_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setBigDecimal(2, amount);
            ps.setBigDecimal(3, balanceAfter);
            ps.setString(4, actor);
            // Sólo un FUNDING lleva respaldo; un CHECK de la base lo confirma.
            if (depositId == null) {
                ps.setNull(5, java.sql.Types.NUMERIC);
            } else {
                ps.setLong(5, depositId);
            }
            ps.executeUpdate();
        }
    }

    /**
     * The Concentrator balance as of a moment in time: the balance_after of the
     * last movement before it.
     *
     * Returns null when the ledger has nothing that old — with no baseline it is
     * better for the screen to show no comparison than to invent a 0 and report
     * an infinite change.
     */
    public BigDecimal balanceAsOf(java.time.LocalDateTime moment) {
        String sql = "SELECT balance_after FROM concentrator_movement "
                   + "WHERE created_at < ? ORDER BY created_at DESC, id DESC "
                   + "FETCH FIRST 1 ROWS ONLY";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(moment));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator history", e);
        }
    }

    /**
     * The last movements of the ledger, newest first.
     *
     * Ordered by created_at AND id: two movements inside the same transaction
     * share a timestamp to the millisecond, and without the tie-break the two
     * rows could swap places between one page load and the next.
     */
    public java.util.List<ConcentratorMovement> findRecent(int limit) {
        return query("SELECT id, movement_type, amount, balance_after, actor, created_at "
                   + "FROM concentrator_movement "
                   + "ORDER BY created_at DESC, id DESC FETCH FIRST ? ROWS ONLY",
                     ps -> ps.setInt(1, limit));
    }

    /** Same, restricted to one movement_type — the reintegrations panel. */
    public java.util.List<ConcentratorMovement> findRecentByType(String type, int limit) {
        return query("SELECT id, movement_type, amount, balance_after, actor, created_at "
                   + "FROM concentrator_movement WHERE movement_type = ? "
                   + "ORDER BY created_at DESC, id DESC FETCH FIRST ? ROWS ONLY",
                     ps -> { ps.setString(1, type); ps.setInt(2, limit); });
    }

    /**
     * Month-to-date figures for the summary panel.
     *
     * TRUNC(SYSDATE, 'MM') is the first of the current month, matching what the
     * rest of the app calls "el mes". The SUMs use NVL so an empty month reads
     * $0.00 instead of null, but the last reintegration stays null on purpose:
     * "never" has to render as a dash, not as a date.
     */
    public ConcentratorSummary summary() {
        String sql = """
                SELECT NVL(SUM(CASE WHEN movement_type = 'DISPERSION'
                                     AND created_at >= TRUNC(SYSDATE, 'MM')
                                    THEN amount END), 0) AS dispersed,
                       COUNT(CASE WHEN created_at >= TRUNC(SYSDATE, 'MM')
                                  THEN 1 END)            AS movements,
                       MAX(CASE WHEN movement_type = 'REINTEGRATION'
                                THEN created_at END)     AS last_reintegration,
                       NVL(SUM(CASE WHEN movement_type = 'REINTEGRATION'
                                     AND created_at >= TRUNC(SYSDATE, 'MM')
                                    THEN amount END), 0) AS reintegrated
                  FROM concentrator_movement
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return new ConcentratorSummary(BigDecimal.ZERO, 0, null, BigDecimal.ZERO);
            }
            java.sql.Timestamp last = rs.getTimestamp("last_reintegration");
            return new ConcentratorSummary(
                    rs.getBigDecimal("dispersed"),
                    rs.getInt("movements"),
                    last == null ? null : last.toLocalDateTime(),
                    rs.getBigDecimal("reintegrated"));
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator summary", e);
        }
    }

    /** Shared plumbing for the two ledger listings above. */
    private java.util.List<ConcentratorMovement> query(String sql, StatementBinder binder) {
        java.util.List<ConcentratorMovement> movements = new java.util.ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp at = rs.getTimestamp("created_at");
                    movements.add(new ConcentratorMovement(
                            rs.getLong("id"),
                            rs.getString("movement_type"),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("actor"),
                            at == null ? null : at.toLocalDateTime()));
                }
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator ledger", e);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
