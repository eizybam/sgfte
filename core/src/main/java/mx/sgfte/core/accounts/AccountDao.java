package mx.sgfte.core.accounts;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public class AccountDao {

    /**
     * One page of the admin accounts table, with the screen's three filters
     * applied. Returns rows only; use {@link #countForAdmin} for the total.
     *
     * The badge colour comes out of the query as a rank over the catalogue
     * ordered by id, wrapped at four. That keeps a purpose the same colour on
     * every page and every screen — deriving it from the row's position in the
     * page, which is what the dashboard does, would repaint the table as you
     * page through it.
     *
     * @param search   matches holder name or account number; null/blank = no filter
     * @param status   ACTIVE, INACTIVE, or null/blank for every status
     * @param purposeId category to restrict to, or null for all
     */
    public List<AccountRow> findForAdmin(String search, String status, Long purposeId,
                                         int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                  "SELECT a.id, a.account_number, a.status, "
                + "       ch.first_name || ' ' || ch.last_name AS holder, "
                + "       cat.name AS purpose, "
                + "       " + mx.sgfte.core.categories.CategoryDao.PURPOSE_COLOR_SQL + " AS purpose_color, "
                + "       a.balance, "
                + "       (SELECT COUNT(*) FROM card c "
                + "         WHERE c.account_id = a.id AND c.status = 'ACTIVE') AS active_cards "
                + "FROM account a "
                + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id ");

        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, purposeId);
        sql.append("ORDER BY a.account_number OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<AccountRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AccountRow(
                            rs.getLong("id"),
                            rs.getString("account_number"),
                            rs.getString("holder"),
                            rs.getString("purpose"),
                            rs.getInt("purpose_color"),
                            rs.getInt("active_cards"),
                            rs.getBigDecimal("balance"),
                            rs.getString("status")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading admin accounts", e);
        }
    }

    /** How many accounts match the same filters — drives the count and the pager. */
    public int countForAdmin(String search, String status, Long purposeId) {
        StringBuilder sql = new StringBuilder(
                  "SELECT COUNT(*) FROM account a "
                + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id ");

        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, purposeId);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting admin accounts", e);
        }
    }

    /**
     * Builds the WHERE shared by the page query and the count so the two can
     * never drift apart. Clauses are only added when the filter is actually set,
     * which keeps the bound types unambiguous for Oracle.
     */
    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search, String status, Long purposeId) {
        sql.append("WHERE 1 = 1 ");

        if (search != null && !search.isBlank()) {
            sql.append("AND (UPPER(ch.first_name || ' ' || ch.last_name) LIKE ? ")
                    .append("  OR UPPER(a.account_number) LIKE ? ")
                    .append("  OR UPPER(ch.employee_code) LIKE ?) ");
            String like = "%" + search.trim().toUpperCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND a.status = ? ");
            params.add(status);
        }
        if (purposeId != null) {
            sql.append("AND cat.id = ? ");
            params.add(purposeId);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /**
     * Every account of one cardholder, for the "Detalle de Tarjetahabiente"
     * panel. Inactive ones are included: the frame lists them with their state
     * rather than hiding them, which is what makes the panel an audit of the
     * person rather than just a list of what still works.
     */
    public List<AccountRow> findByCardholder(long cardholderId) {
        String sql = "SELECT a.id, a.account_number, a.status, a.balance, "
                   + "       ch.first_name || ' ' || ch.last_name AS holder, "
                   + "       cat.name AS purpose, "
                   + "       " + mx.sgfte.core.categories.CategoryDao.PURPOSE_COLOR_SQL + " AS purpose_color, "
                   + "       (SELECT COUNT(*) FROM card c "
                   + "         WHERE c.account_id = a.id AND c.status = 'ACTIVE') AS active_cards "
                   + "FROM account a "
                   + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                   + "JOIN category  cat ON cat.id = a.category_id "
                   + "WHERE a.cardholder_id = ? "
                   + "ORDER BY a.status, cat.name";
        List<AccountRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AccountRow(
                            rs.getLong("id"), rs.getString("account_number"),
                            rs.getString("holder"), rs.getString("purpose"),
                            rs.getInt("purpose_color"), rs.getInt("active_cards"),
                            rs.getBigDecimal("balance"), rs.getString("status")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts of cardholder", e);
        }
    }

    /**
     * Everything the account detail header needs in one read.
     *
     * The badge colour is computed exactly as in the listing — a rank over the
     * catalogue ordered by id — so an account keeps the same colour whether you
     * are looking at the table or at its detail page.
     */
    public Optional<AccountDetail> findDetail(long accountId) {
        String sql = "SELECT a.id, a.account_number, a.balance, a.status, "
                   + "       cat.name AS purpose, "
                   + "       ch.first_name || ' ' || ch.last_name AS holder, "
                   + "       " + mx.sgfte.core.categories.CategoryDao.PURPOSE_COLOR_SQL + " AS purpose_color "
                   + "FROM account a "
                   + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                   + "JOIN category  cat ON cat.id = a.category_id "
                   + "WHERE a.id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new AccountDetail(
                        rs.getLong("id"),
                        rs.getString("account_number"),
                        rs.getString("purpose"),
                        rs.getInt("purpose_color"),
                        rs.getString("holder"),
                        rs.getBigDecimal("balance"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading account detail", e);
        }
    }

    /**
     * The month panel, in a single round trip.
     *
     * "Dispersado" and "Movimientos" are scoped to the current calendar month;
     * "Última recarga" is the newest deposit ever, since the frame shows a full
     * date and an empty month should still tell you when funds last arrived.
     */
    public AccountMonthSummary monthSummary(long accountId) {
        String sql = "SELECT "
                   + " (SELECT NVL(SUM(amount), 0) FROM account_movement "
                   + "   WHERE account_id = ? AND movement_type = 'DEPOSIT' "
                   + "     AND created_at >= TRUNC(SYSDATE, 'MM')) AS dispersed, "
                   + " (SELECT COUNT(*) FROM account_movement "
                   + "   WHERE account_id = ? AND created_at >= TRUNC(SYSDATE, 'MM')) AS movements, "
                   + " (SELECT MAX(created_at) FROM account_movement "
                   + "   WHERE account_id = ? AND movement_type = 'DEPOSIT') AS last_deposit, "
                   + " (SELECT COUNT(*) FROM card "
                   + "   WHERE account_id = ? AND status = 'ACTIVE') AS active_cards "
                   + "FROM dual";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setLong(i, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new AccountMonthSummary(BigDecimal.ZERO, 0, null, 0);
                }
                java.sql.Timestamp last = rs.getTimestamp("last_deposit");
                return new AccountMonthSummary(
                        rs.getBigDecimal("dispersed"),
                        rs.getInt("movements"),
                        last == null ? null : last.toLocalDateTime(),
                        rs.getInt("active_cards"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading account month summary", e);
        }
    }

    /** True if the cardholder already has an account with this purpose. */
    public boolean existForPurpose(Long cardholderId, long categoryId) {
        String sql = "SELECT 1 FROM account WHERE cardholder_id = ? AND category_id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, cardholderId);
            preparedStatement.setLong(2, categoryId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking account", e);
        }
    }

    /**
     * Inserts an account with its business code (account_number).
     * Sets the generated numeric id back on the object and also returns it.
     * NOTE: account.getAccountNumber() must be set (generated in the service) before calling this.
     *
     * If the UNIQUE constraint on account_number is violated, throws
     * DuplicateAccountNumberException so the service can regenerate and retry.
     */
    public long insert(Account account) {
        String sql = "INSERT INTO account (account_number, cardholder_id, category_id) VALUES (?, ?, ?)";
        try (Connection connection = Db.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[]{"id"})) {
            preparedStatement.setString(1, account.getAccountNumber());
            preparedStatement.setLong(2, account.getCardholderId());
            preparedStatement.setLong(3, account.getCategoryId());
            preparedStatement.executeUpdate();
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    account.setId(id);
                    return id;
                }
            }
            throw new IllegalStateException("Insert succeeded but no generated id was returned");
        } catch (SQLIntegrityConstraintViolationException e) {
            // Oracle ORA-00001. Distinguish WHICH unique constraint failed by name.
            String msg = e.getMessage() == null ? "" : e.getMessage().toUpperCase();
            if (msg.contains("UQ_ACCOUNT_NUMBER")) {
                throw new DuplicateAccountNumberException(account.getAccountNumber(), e);
            }
            // e.g. UQ_ACCOUNT_PURPOSE — a real conflict a retry can't fix; let it surface.
            throw new RuntimeException("Constraint violation inserting account", e);
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting account", e);
        }
    }

    /**
     * Looks up an account by its public business code (e.g. "GAS-48HSY").
     * Powers the P2P transfer flow: the sender types the code, we resolve the numeric id.
     */
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT id, account_number, cardholder_id, category_id, balance, status "
                + "FROM account WHERE account_number = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, accountNumber);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up account by number", e);
        }
    }

    /** Maps a full account row into an Account entity. */
    private Account mapRow(ResultSet rs) throws SQLException {
        Account a = new Account(rs.getLong("cardholder_id"), rs.getLong("category_id"));
        a.setId(rs.getLong("id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setActive("ACTIVE".equals(rs.getString("status")));
        return a;
    }

    /**
     * Adds money to an ACTIVE account inside a transaction.
     * @return true if it affected 1 row (account exists and is active); false otherwise.
     */
    public boolean credit(java.sql.Connection conn, long accountId, java.math.BigDecimal amount)
            throws java.sql.SQLException {
        String sql = "UPDATE account SET balance = balance + ? WHERE id = ? AND status = 'ACTIVE'";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * (Used by Modules 3 and 4) Debits an ACTIVE account with enough balance.
     * @return true if it affected 1 row (there was balance); false otherwise.
     */
    public boolean debit(java.sql.Connection conn, long accountId, java.math.BigDecimal amount)
            throws java.sql.SQLException {
        String sql = "UPDATE account SET balance = balance - ? WHERE id = ? AND status = 'ACTIVE' AND balance >= ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate() == 1;
        }
    }
}
