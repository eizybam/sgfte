package mx.sgfte.core.accounts;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;
import java.math.BigDecimal;

public class AccountDao {

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
