package mx.sgfte.core.users;

import mx.sgfte.core.shared.db.Db;
import oracle.jdbc.proxy.annotation.Pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Persistence for cardholders (JDBC over Oracle). */
public class CardholderDao {

    /**
     * One page of the employee table, with the screen's search and status
     * filters. Accounts, cards and funds are counted per cardholder.
     *
     * The three totals are scalar subqueries rather than joins on purpose:
     * joining cardholder → account → card multiplies each account row by its
     * card count, and summing balances over that fan-out would inflate the
     * fund total. (SUM(DISTINCT) is not a fix either — it would silently drop
     * a second account that happens to hold the same amount.)
     */
    public List<CardholderAdminRow> findForAdmin(String search, String status, String department,
                                                 int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                  "SELECT ch.id, ch.first_name, ch.last_name, ch.status, "
                + "       ch.employee_code, ch.email, "
                + "  (SELECT COUNT(*) FROM account a "
                + "    WHERE a.cardholder_id = ch.id AND a.status = 'ACTIVE') AS accounts, "
                + "  (SELECT COUNT(*) FROM card k JOIN account ka ON ka.id = k.account_id "
                + "    WHERE ka.cardholder_id = ch.id AND ka.status = 'ACTIVE' "
                + "      AND k.status = 'ACTIVE') AS cards, "
                + "  (SELECT NVL(SUM(af.balance), 0) FROM account af "
                + "    WHERE af.cardholder_id = ch.id AND af.status = 'ACTIVE') AS funds "
                + "FROM cardholder ch ");

        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, department);

        sql.append("ORDER BY ch.last_name, ch.first_name ")
           .append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<CardholderAdminRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new CardholderAdminRow(
                            rs.getLong("id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("employee_code"),
                            rs.getString("email"),
                            rs.getInt("accounts"),
                            rs.getInt("cards"),
                            rs.getBigDecimal("funds"),
                            rs.getString("status")));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading admin cardholders", e);
        }
    }

    /** How many cardholders match the same filters — drives the count and pager. */
    public int countForAdmin(String search, String status, String department) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM cardholder ch ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, department);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting admin cardholders", e);
        }
    }

    /** Shared WHERE so the page query and the count can never drift apart. */
    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search, String status, String department) {
        sql.append("WHERE 1 = 1 ");

        if (search != null && !search.isBlank()) {
            // "Buscar por nombre o ID de empleado": el ID es el código, no la
            // clave primaria. El correo se deja porque cuesta nada y ayuda.
            sql.append("AND (UPPER(ch.first_name || ' ' || ch.last_name) LIKE ? ")
               .append("  OR UPPER(ch.employee_code) LIKE ? ")
               .append("  OR UPPER(ch.email) LIKE ?) ");
            String like = "%" + search.trim().toUpperCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND ch.status = ? ");
            params.add(status);
        }
        if (department != null && !department.isBlank()) {
            sql.append("AND ch.department = ? ");
            params.add(department);
        }
    }

    /** The departments actually in use — feeds the toolbar pill. */
    public List<String> distinctDepartments() {
        String sql = "SELECT DISTINCT department FROM cardholder "
                   + "WHERE department IS NOT NULL ORDER BY department";
        List<String> names = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString(1));
            return names;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading departments", e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /** True if a cardholder with this email already exists. */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM cardholder WHERE email = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email", e);
        }
    }

    /** Inserts a cardholder and returns the generated id. */
    /**
     * The next value of seq_employee_code.
     *
     * Pulled separately so the code is built in the service, where the rule can
     * be unit-tested without a database.
     */
    public long nextEmployeeSequence() {
        String sql = "SELECT seq_employee_code.NEXTVAL FROM dual";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            throw new IllegalStateException("seq_employee_code returned no value");
        } catch (SQLException e) {
            throw new RuntimeException("Error reading employee code sequence", e);
        }
    }

    public long insert(Cardholder ch) {
        String sql = "INSERT INTO cardholder "
                   + "(first_name, last_name, email, phone, employee_code, department) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, ch.getFirstName());
            ps.setString(2, ch.getLastName());
            ps.setString(3, ch.getEmail());
            ps.setString(4, ch.getPhone());
            ps.setString(5, ch.getEmployeeCode());
            ps.setString(6, ch.getDepartment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("Insert succeeded but no generated id was returned");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting cardholder", e);
        }
    }

    public List<Cardholder> findAllActive() {
        String sql = "SELECT id, first_name, last_name FROM cardholder WHERE status = 'ACTIVE' ORDER BY last_name, first_name";
        List<Cardholder> cardholders = new ArrayList<>();
        try (Connection connection = Db.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Cardholder cardholder = new Cardholder();
                cardholder.setId(resultSet.getLong("id"));
                cardholder.setFirstName(resultSet.getString("first_name"));
                cardholder.setLastName(resultSet.getString("last_name"));

                cardholders.add(cardholder);
            }
            return cardholders;

        } catch (SQLException e) {
            throw new RuntimeException("Error loading cardholders", e);
        }
    }
}
