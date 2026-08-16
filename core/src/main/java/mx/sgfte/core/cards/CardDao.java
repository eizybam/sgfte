package mx.sgfte.core.cards;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistence for cards. */
public class CardDao {

    public long insert(Card card) {
        String sql = "INSERT INTO card (account_id, card_type, masked_pan, expires_at) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setLong(1, card.getAccountId());
            ps.setString(2, card.getCardType());
            ps.setString(3, card.getMaskedPan());
            ps.setDate(4, java.sql.Date.valueOf(card.getExpiresAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new IllegalStateException("Insert succeeded but no id returned");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting card", e);
        }
    }

    /** All cards of an account (for the account view). */
    public List<Card> findByAccount(long accountId) {
        String sql = "SELECT id, account_id, card_type, masked_pan, status, created_at, expires_at "
                + "FROM card WHERE account_id = ? ORDER BY id";
        List<Card> cards = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = new Card();
                    card.setIssuedAt(toLocalDate(rs.getTimestamp("created_at")));
                    card.setExpiresAt(toLocalDate(rs.getDate("expires_at")));
                    card.setId(rs.getLong("id"));
                    card.setAccountId(rs.getLong("account_id"));
                    card.setCardType(rs.getString("card_type"));
                    card.setMaskedPan(rs.getString("masked_pan"));
                    card.setStatus(rs.getString("status"));
                    cards.add(card);
                }
            }
            return cards;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cards", e);
        }
    }

    /** Invalidates one card (soft: status -> INACTIVE). Deleting a card does NOT move money. */
    public boolean invalidate(long cardId) {
        String sql = "UPDATE card SET status = 'INACTIVE' WHERE id = ? AND status = 'ACTIVE'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error invalidating card", e);
        }
    }

    /**
     * Every active account a card can be issued against, joined with its holder,
     * its number and its purpose — what the "Expedir Tarjeta" screen displays.
     *
     * Ordered by holder then purpose so the two dropdowns read alphabetically.
     */
    public List<IssueTarget> findIssueTargets(long cardholderId) {
        String sql = "SELECT a.id, a.account_number, a.balance, "
                + "       cat.name AS purpose, "
                + "       ch.id AS cardholder_id, ch.first_name, ch.last_name "
                + "FROM account a "
                + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id "
                + "WHERE a.status = 'ACTIVE' AND ch.status = 'ACTIVE' "
                + "  AND ch.id = ? "
                + "ORDER BY cat.name";
        List<IssueTarget> targets = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    targets.add(new IssueTarget(
                            rs.getLong("id"),
                            rs.getString("account_number"),
                            rs.getString("purpose"),
                            rs.getBigDecimal("balance"),
                            rs.getLong("cardholder_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name")));
                }
            }
            return targets;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading card issue targets", e);
        }
    }

    public Optional<IssueTarget> findIssueTarget(long accountId) {
        String sql = "SELECT a.id, a.account_number, a.balance, "
                + "       cat.name AS purpose, "
                + "       ch.id AS cardholder_id, ch.first_name, ch.last_name "
                + "FROM account a "
                + "JOIN cardholder ch ON ch.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id "
                + "WHERE a.id = ? AND a.status = 'ACTIVE' AND ch.status = 'ACTIVE'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new IssueTarget(
                        rs.getLong("id"),
                        rs.getString("account_number"),
                        rs.getString("purpose"),
                        rs.getBigDecimal("balance"),
                        rs.getLong("cardholder_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading issue target " + accountId, e);
        }
    }

    /**
     * Every card belonging to one cardholder, across all of their accounts.
     *
     * The frame's "Tarjetas vinculadas" panel is about the person, not about a
     * single account, so the join goes through account rather than filtering by
     * account_id.
     */
    public List<Card> findByCardholder(long cardholderId) {
        String sql = "SELECT k.id, k.account_id, k.card_type, k.masked_pan, k.status, k.created_at, k.expires_at "
                   + "FROM card k JOIN account a ON a.id = k.account_id "
                   + "WHERE a.cardholder_id = ? ORDER BY k.status, k.id";
        List<Card> cards = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = new Card();
                    card.setIssuedAt(toLocalDate(rs.getTimestamp("created_at")));
                    card.setExpiresAt(toLocalDate(rs.getDate("expires_at")));
                    card.setId(rs.getLong("id"));
                    card.setAccountId(rs.getLong("account_id"));
                    card.setCardType(rs.getString("card_type"));
                    card.setMaskedPan(rs.getString("masked_pan"));
                    card.setStatus(rs.getString("status"));
                    cards.add(card);
                }
            }
            return cards;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cards of cardholder", e);
        }
    }

    /** True if the account exists and is ACTIVE (so we don't issue cards to dead accounts). */
    public boolean isAccountActive(long accountId) {
        String sql = "SELECT 1 FROM account WHERE id = ? AND status = 'ACTIVE'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking account", e);
        }
    }

    /**
     * (Used by Module 4) Invalidates ALL cards of an account inside a transaction —
     * when the account/cardholder is deleted.
     */
    public void invalidateAllForAccount(Connection conn, long accountId) throws SQLException {
        String sql = "UPDATE card SET status = 'INACTIVE' WHERE account_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.executeUpdate();
        }
    }

    /** Las dos columnas de fecha llegan con tipos distintos; una sola salida. */
    private java.time.LocalDate toLocalDate(java.util.Date value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        return new java.sql.Date(value.getTime()).toLocalDate();
    }
}
