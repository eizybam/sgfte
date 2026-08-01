package mx.sgfte.core.cards;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Persistence for cards. */
public class CardDao {

    public long insert(Card card) {
        String sql = "INSERT INTO card (account_id, card_type, masked_pan) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setLong(1, card.getAccountId());
            ps.setString(2, card.getCardType());
            ps.setString(3, card.getMaskedPan());
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
        String sql = "SELECT id, account_id, card_type, masked_pan, status "
                + "FROM card WHERE account_id = ? ORDER BY id";
        List<Card> cards = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Card card = new Card();
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
}
