package mx.sgfte.core.movements;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read side of the ledger: the history of one account, and the global view
 * across both books (v_movement, V11).
 */
public class MovementQueryDao {

    public List<Movement> findByAccount(long accountId) {
        String sql = "SELECT id, account_id, movement_type, amount, related_account_id, description, created_at "
                   + "FROM account_movement WHERE account_id = ? ORDER BY created_at DESC, id DESC";
        List<Movement> list = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movement m = new Movement();
                    m.setId(rs.getLong("id"));
                    m.setAccountId(rs.getLong("account_id"));
                    m.setMovementType(rs.getString("movement_type"));
                    m.setAmount(rs.getBigDecimal("amount"));
                    long rel = rs.getLong("related_account_id");
                    m.setRelatedAccountId(rs.wasNull() ? null : rel);
                    m.setDescription(rs.getString("description"));
                    m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(m);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading movement history", e);
        }
    }

    // ---- Vista global de movimientos (/admin/movimientos) -------------------

    /*
      Las columnas se escriben una vez porque la página y el conteo tienen que
      mirar exactamente el mismo conjunto. Cuando divergen, el paginador promete
      páginas que no existen.
     */
    private static final String GLOBAL_COLUMNS =
              "scope, source_id, movement_type, amount, direction, description, "
            + "account_id, account_number, holder, employee_code, "
            + "category_id, category_name, color_index, related_number, actor, created_at ";

    /**
     * El WHERE que comparten la página y el conteo.
     *
     * Ojo con los filtros de cuenta y de propósito: las filas de la
     * Concentradora tienen esas columnas en NULL, así que aplicar cualquiera de
     * los dos las excluye por sí solo. Es lo correcto —un fondeo no pertenece a
     * ninguna cuenta ni a ningún propósito— y por eso no hace falta añadir nada
     * sobre el ámbito.
     */
    private void appendGlobalFilters(StringBuilder sql, List<Object> params,
                                     String search, String scope, String type,
                                     Long categoryId, Long accountId, String period) {
        sql.append("WHERE 1 = 1 ");

        if (search != null && !search.isBlank()) {
            sql.append("AND (UPPER(description) LIKE ? OR UPPER(account_number) LIKE ? ")
               .append("  OR UPPER(holder) LIKE ? OR UPPER(employee_code) LIKE ? ")
               .append("  OR UPPER(actor) LIKE ?) ");
            String like = "%" + search.trim().toUpperCase() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        if (scope != null && !scope.isBlank()) {
            sql.append("AND scope = ? ");
            params.add(scope);
        }
        if (type != null && !type.isBlank()) {
            sql.append("AND movement_type = ? ");
            params.add(type);
        }
        if (categoryId != null) {
            sql.append("AND category_id = ? ");
            params.add(categoryId);
        }
        if (accountId != null) {
            sql.append("AND account_id = ? ");
            params.add(accountId);
        }
        // TODOS no añade nada: es el filtro de fecha apagado.
        switch (period == null ? "" : period) {
            case "HOY" -> sql.append("AND created_at >= TRUNC(SYSDATE) ");
            case "7D"  -> sql.append("AND created_at >= TRUNC(SYSDATE) - 7 ");
            case "30D" -> sql.append("AND created_at >= TRUNC(SYSDATE) - 30 ");
            default    -> { }
        }
    }

    /**
     * Una página de la vista global, lo más nuevo primero.
     *
     * El desempate es (created_at, scope, source_id) y no sólo el id: los dos
     * ledgers numeran por separado, así que un id no ordena nada entre libros.
     * Y hace falta desempatar porque una dispersión escribe en los dos dentro
     * de la MISMA transacción, con el mismo instante al milisegundo: sin esto
     * las dos filas podrían intercambiarse entre una carga y la siguiente, que
     * es justo como un paginador se salta o repite renglones.
     */
    public List<GlobalMovementRow> findGlobal(String search, String scope, String type,
                                              Long categoryId, Long accountId, String period,
                                              int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT " + GLOBAL_COLUMNS + "FROM v_movement ");
        List<Object> params = new ArrayList<>();
        appendGlobalFilters(sql, params, search, scope, type, categoryId, accountId, period);
        sql.append("ORDER BY created_at DESC, scope, source_id DESC ")
           .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<GlobalMovementRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(readGlobal(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the global movement view", e);
        }
    }

    public int countGlobal(String search, String scope, String type,
                           Long categoryId, Long accountId, String period) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM v_movement ");
        List<Object> params = new ArrayList<>();
        appendGlobalFilters(sql, params, search, scope, type, categoryId, accountId, period);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting the global movement view", e);
        }
    }

    /**
     * Los tipos que de verdad existen en los datos — alimenta la píldora.
     *
     * Salen de la vista y no de una lista escrita a mano por lo mismo que los
     * módulos de la bitácora: ofrecer un filtro que sólo puede devolver cero
     * filas es prometer algo que no hay.
     */
    public List<String> distinctTypes() {
        String sql = "SELECT DISTINCT movement_type FROM v_movement ORDER BY movement_type";
        List<String> types = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) types.add(rs.getString(1));
            return types;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading movement types", e);
        }
    }

    private GlobalMovementRow readGlobal(ResultSet rs) throws SQLException {
        long accId = rs.getLong("account_id");
        Long accountId = rs.wasNull() ? null : accId;
        int color = rs.getInt("color_index");
        Integer colorIndex = rs.wasNull() ? null : color;
        java.sql.Timestamp at = rs.getTimestamp("created_at");

        return new GlobalMovementRow(
                rs.getString("scope"),
                rs.getLong("source_id"),
                rs.getString("movement_type"),
                rs.getBigDecimal("amount"),
                rs.getString("direction"),
                rs.getString("description"),
                accountId,
                rs.getString("account_number"),
                rs.getString("holder"),
                rs.getString("employee_code"),
                rs.getString("category_name"),
                colorIndex,
                rs.getString("related_number"),
                rs.getString("actor"),
                at == null ? null : at.toLocalDateTime());
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }
}
