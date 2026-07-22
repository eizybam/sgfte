package mx.sgfte.core.accounts;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AccountDao {

    /** True if the cardholder already has an account with this purpose. */
    public boolean existForPurpose(Long cardholderId, long categoryId) {
        String sql = "SELECT 1 FROM account WHERE cardholder_id = ? AND category_id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setLong(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
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
     */
    public long insert(Account account) {
        String sql = "INSERT INTO account (account_number, cardholder_id, category_id) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, account.getAccountNumber());
            ps.setLong(2, account.getCardholderId());
            ps.setLong(3, account.getCategoryId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    account.setId(id);
                    return id;
                }
            }
            throw new IllegalStateException("Insert succeeded but no generated id was returned");
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
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
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
}
