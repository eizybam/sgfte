package mx.sgfte.core.analytics;

import mx.sgfte.core.categories.CategoryDao;
import mx.sgfte.core.shared.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads for the Analíticas screen (Figma 2029:6).
 *
 * Every query is scoped by an explicit [from, to) window, because the page's
 * period selector re-scopes the whole screen — including the "vs periodo
 * anterior" deltas, which run the same query over the preceding window.
 *
 * Separate from DashboardDao on purpose: that one answers "what is true right
 * now" for the Vista Global, this one answers "what happened between two
 * moments". Read-only throughout.
 */
public class AnalyticsDao {

    /** Dispersed (DEPOSIT) inside the window. */
    public BigDecimal dispersedBetween(LocalDateTime from, LocalDateTime to) {
        return decimal("SELECT NVL(SUM(amount), 0) FROM account_movement "
                     + "WHERE movement_type = 'DEPOSIT' AND created_at >= ? AND created_at < ?",
                       from, to);
    }

    /** Cardholders registered inside the window. */
    public int newCardholdersBetween(LocalDateTime from, LocalDateTime to) {
        return count("SELECT COUNT(*) FROM cardholder WHERE created_at >= ? AND created_at < ?",
                     from, to);
    }

    /** Active cards split by type — the frame's "172 físicas · 42 digitales". */
    public Map<String, Integer> activeCardsByType() {
        Map<String, Integer> byType = new LinkedHashMap<>();
        byType.put("PHYSICAL", 0);
        byType.put("DIGITAL", 0);

        String sql = "SELECT card_type, COUNT(*) AS n FROM card "
                   + "WHERE status = 'ACTIVE' GROUP BY card_type";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) byType.put(rs.getString("card_type"), rs.getInt("n"));
            return byType;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cards by type", e);
        }
    }

    /**
     * Dispersion per bucket across the window, oldest first.
     *
     * Buckets are built in Java from a single grouped read rather than twelve
     * queries; a bucket with no movements has to come back as zero, and letting
     * SQL do that would need a generated calendar it does not have.
     */
    public BigDecimal[] dispersionBuckets(LocalDateTime from, LocalDateTime to,
                                          int buckets, boolean byMonth) {
        BigDecimal[] totals = new BigDecimal[buckets];
        java.util.Arrays.fill(totals, BigDecimal.ZERO);

        String sql = "SELECT created_at, amount FROM account_movement "
                   + "WHERE movement_type = 'DEPOSIT' AND created_at >= ? AND created_at < ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime at = rs.getTimestamp("created_at").toLocalDateTime();
                    int slot = bucketOf(at, from, to, buckets, byMonth);
                    if (slot >= 0 && slot < buckets) {
                        totals[slot] = totals[slot].add(rs.getBigDecimal("amount"));
                    }
                }
            }
            return totals;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading dispersion buckets", e);
        }
    }

    /** Which bucket a moment falls into. Months are counted, days are divided. */
    private int bucketOf(LocalDateTime at, LocalDateTime from, LocalDateTime to,
                         int buckets, boolean byMonth) {
        if (byMonth) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                    from.toLocalDate().withDayOfMonth(1), at.toLocalDate().withDayOfMonth(1));
            return (int) months;
        }
        long span = java.time.temporal.ChronoUnit.SECONDS.between(from, to);
        if (span <= 0) return 0;
        long offset = java.time.temporal.ChronoUnit.SECONDS.between(from, at);
        return (int) (offset * buckets / span);
    }

    /**
     * Movement counts by day of week inside the window, Monday first.
     *
     * TO_CHAR(...,'D') depends on NLS_TERRITORY, so the day is derived in Java
     * from the timestamp instead — otherwise the chart would silently shift by a
     * day on a differently configured database.
     */
    public int[] movementsByWeekday(LocalDateTime from, LocalDateTime to) {
        int[] perDay = new int[7];   // 0 = lunes
        String sql = "SELECT created_at FROM account_movement "
                   + "WHERE created_at >= ? AND created_at < ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime at = rs.getTimestamp(1).toLocalDateTime();
                    perDay[at.getDayOfWeek().getValue() - 1]++;
                }
            }
            return perDay;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading weekday activity", e);
        }
    }

    /** Dispersion per purpose inside the window, biggest first. */
    public Map<String, BigDecimal> dispersionByPurpose(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT cat.name AS purpose, NVL(SUM(m.amount), 0) AS total "
                   + "FROM account_movement m "
                   + "JOIN account  a  ON a.id = m.account_id "
                   + "JOIN category cat ON cat.id = a.category_id "
                   + "WHERE m.movement_type = 'DEPOSIT' AND m.created_at >= ? AND m.created_at < ? "
                   + "GROUP BY cat.name ORDER BY total DESC";
        return purposeMap(sql, from, to);
    }

    /** P2P transfers per purpose inside the window, biggest first. */
    public Map<String, BigDecimal> transfersByPurpose(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT cat.name AS purpose, NVL(SUM(m.amount), 0) AS total "
                   + "FROM account_movement m "
                   + "JOIN account  a  ON a.id = m.account_id "
                   + "JOIN category cat ON cat.id = a.category_id "
                   + "WHERE m.movement_type = 'TRANSFER_OUT' AND m.created_at >= ? AND m.created_at < ? "
                   + "GROUP BY cat.name ORDER BY total DESC";
        return purposeMap(sql, from, to);
    }

    /** Total and count of P2P transfers inside the window. */
    public BigDecimal transfersTotal(LocalDateTime from, LocalDateTime to) {
        return decimal("SELECT NVL(SUM(amount), 0) FROM account_movement "
                     + "WHERE movement_type = 'TRANSFER_OUT' AND created_at >= ? AND created_at < ?",
                       from, to);
    }

    public int transfersCount(LocalDateTime from, LocalDateTime to) {
        return count("SELECT COUNT(*) FROM account_movement "
                   + "WHERE movement_type = 'TRANSFER_OUT' AND created_at >= ? AND created_at < ?",
                     from, to);
    }

    /**
     * Cardholders who received the most inside the window.
     *
     * Grouped by cardholder, not by account: the frame lists people, and someone
     * with three accounts should appear once with their total, not three times.
     */
    public List<Object[]> topSpenders(LocalDateTime from, LocalDateTime to, int limit) {
        /*
          El color se calcula en una consulta interna, fila a fila, y sólo
          después se agrupa. Meterlo dentro del agregado del mismo bloque —como
          estaba— dejaba una subconsulta correlacionada con `cat`, que no está
          en el GROUP BY; así queda fuera de duda.

          MIN(color): un tarjetahabiente puede tener cuentas de varios
          propósitos y la barra sólo pinta uno. Se toma el primero del catálogo
          para que no cambie entre recargas.
        */
        String sql = "SELECT holder, SUM(amount) AS total, MIN(color) AS color FROM ( "
                   + "  SELECT ch.id AS holder_id, "
                   + "         ch.first_name || ' ' || ch.last_name AS holder, "
                   + "         m.amount AS amount, "
                   + "         " + CategoryDao.PURPOSE_COLOR_SQL + " AS color "
                   + "  FROM account_movement m "
                   + "  JOIN account    a   ON a.id = m.account_id "
                   + "  JOIN cardholder ch  ON ch.id = a.cardholder_id "
                   + "  JOIN category   cat ON cat.id = a.category_id "
                   + "  WHERE m.movement_type = 'DEPOSIT' "
                   + "    AND m.created_at >= ? AND m.created_at < ? "
                   + ") GROUP BY holder_id, holder "
                   + "ORDER BY total DESC "
                   + "FETCH FIRST ? ROWS ONLY";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString("holder"),
                                          rs.getBigDecimal("total"),
                                          rs.getInt("color")});
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading top spenders", e);
        }
    }

    private Map<String, BigDecimal> purposeMap(String sql, LocalDateTime from, LocalDateTime to) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("purpose"), rs.getBigDecimal("total"));
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading purpose split", e);
        }
    }

    private BigDecimal decimal(String sql, LocalDateTime from, LocalDateTime to) {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in query: " + sql, e);
        }
    }

    private int count(String sql, LocalDateTime from, LocalDateTime to) {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in query: " + sql, e);
        }
    }
}
