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
    private static final String ADMIN_SELECT =
            "SELECT ch.id, ch.first_name, ch.last_name, ch.status, "
                    + "       ch.employee_code, ch.email, "
                    + "  (SELECT COUNT(*) FROM account a "
                    + "    WHERE a.cardholder_id = ch.id AND a.status = 'ACTIVE') AS accounts, "
                    + "  (SELECT COUNT(*) FROM card k JOIN account ka ON ka.id = k.account_id "
                    + "    WHERE ka.cardholder_id = ch.id AND ka.status = 'ACTIVE' "
                    + "      AND k.status = 'ACTIVE') AS cards, "
                    + "  (SELECT NVL(SUM(af.balance), 0) FROM account af "
                    + "    WHERE af.cardholder_id = ch.id AND af.status = 'ACTIVE') AS funds "
                    + "FROM cardholder ch ";

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
        StringBuilder sql = new StringBuilder(ADMIN_SELECT);

        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, department);

        sql.append("ORDER BY ch.status, ch.last_name, ch.first_name ")
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

    /**
     * Una página del selector de empleados.
     *
     * `onlyWithAccounts` NO es un adorno: distingue las dos pantallas que abren
     * este selector, y confundirlas se nota.
     *
     *   · Expedir Tarjeta pide true. Una tarjeta se expide CONTRA una cuenta;
     *     ofrecer a alguien que no tiene ninguna es ofrecer un callejón sin
     *     salida.
     *
     *   · Crear cuenta pide false, y es justo al revés: ahí se busca a quien le
     *     falta una. Con el filtro puesto, un empleado recién dado de alta —que
     *     por definición tiene cero cuentas— no aparecía nunca, y no había forma
     *     de darle la primera.
     */
    public List<CardholderAdminRow> findForPicker(String search, int offset, int limit,
                                                  boolean onlyWithAccounts) throws SQLException {
        StringBuilder sql = new StringBuilder(ADMIN_SELECT);
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, "ACTIVE", null, onlyWithAccounts);

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
            throw new RuntimeException("Error loading cardholders for picker", e);
        }
    }

    /** Cuántos casan con lo mismo que findForPicker — mismo criterio o el pager miente. */
    public int countForPicker(String search, boolean onlyWithAccounts) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM cardholder ch ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, "ACTIVE", null, onlyWithAccounts);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting cardholders for picker", e);
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

    /**
     * Header and profile summary for one cardholder, in a single read.
     *
     * The totals are scalar subqueries for the same reason as the listing:
     * joining through account to card multiplies the rows and would inflate the
     * balance.
     */
    public java.util.Optional<CardholderDetail> findDetail(long cardholderId) {
        String sql = "SELECT ch.id, ch.employee_code, ch.first_name, ch.last_name, "
                   + "       ch.email, ch.phone, d.name AS department, ch.status, "
                   + "  (SELECT NVL(SUM(a.balance), 0) FROM account a "
                   + "    WHERE a.cardholder_id = ch.id AND a.status = 'ACTIVE') AS total_balance, "
                   + "  (SELECT COUNT(*) FROM account a "
                   + "    WHERE a.cardholder_id = ch.id AND a.status = 'ACTIVE') AS active_accounts, "
                   + "  (SELECT COUNT(*) FROM card k JOIN account ka ON ka.id = k.account_id "
                   + "    WHERE ka.cardholder_id = ch.id AND k.status = 'ACTIVE') AS card_count "
                   + "FROM cardholder ch "
                   + "  LEFT JOIN department d ON d.id = ch.department_id "
                   + " WHERE ch.id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                return java.util.Optional.of(new CardholderDetail(
                        rs.getLong("id"),
                        rs.getString("employee_code"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("department"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_balance"),
                        rs.getInt("active_accounts"),
                        rs.getInt("card_count")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cardholder detail", e);
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
            sql.append("AND ch.department_id = (SELECT d.id FROM department d WHERE d.name = ?) ");
            params.add(department);
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search, String status, String department, boolean onlyIssuable) {
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
            sql.append("AND ch.department_id = (SELECT d.id FROM department d WHERE d.name = ?) ");
            params.add(department);
        }

        if (onlyIssuable) {
            sql.append("AND EXISTS (SELECT 1 FROM account a ")
                    .append("       WHERE a.cardholder_id = ch.id ")
                    .append("           AND a.status = 'ACTIVE') ");
        }

    }

    /**
     * Los departamentos que se pueden filtrar en la píldora de la barra.
     *
     * Sale del catálogo y no de un DISTINCT sobre cardholder: desde V10 el área
     * es una FK, así que la lista de áreas es el catálogo. Un DISTINCT dejaría
     * fuera un área recién creada a la que todavía no pertenece nadie, y la
     * píldora enseñaría menos opciones de las que existen.
     */
    public List<String> distinctDepartments() {
        String sql = "SELECT name FROM department WHERE status = 'ACTIVE' ORDER BY name";
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

    /** department_id es NULL-able: un empleado puede no tener área asignada. */
    private void setDepartment(PreparedStatement ps, int index, Long departmentId)
            throws SQLException {
        if (departmentId == null) ps.setNull(index, java.sql.Types.NUMERIC);
        else ps.setLong(index, departmentId);
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /**
     * True if a cardholder with this email already exists, ignoring case.
     *
     * El UPPER de los dos lados es el que faltaba: con "email = ?" se colaba
     * "mAIL@empresa.com" junto a "mail@empresa.com". emailExistsForAnother —el
     * gemelo que usa la edición— ya lo hacía; el del alta se quedó atrás.
     *
     * Sigue haciendo falta aunque el servicio ya guarde en minúsculas: en la
     * base hay filas anteriores a esa normalización, y esta consulta también
     * tiene que verlas.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM cardholder WHERE UPPER(email) = UPPER(?)";
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
                   + "(first_name, last_name, email, phone, employee_code, department_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, ch.getFirstName());
            ps.setString(2, ch.getLastName());
            ps.setString(3, ch.getEmail());
            ps.setString(4, ch.getPhone());
            ps.setString(5, ch.getEmployeeCode());
            setDepartment(ps, 6, ch.getDepartmentId());
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

    /**
     * Cómo se lee UN empleado en el campo del selector: "Ana Ramírez · ARM0042".
     *
     * Existe por el reintento del alta de cuenta. Antes el empleado elegido se
     * recuperaba solo, porque estaban TODOS en el <select> y bastaba con marcar
     * el suyo; con el selector con tabla el desplegable ya no existe, así que
     * hay que traer su etiqueta — y sólo la suya, no la lista entera. Misma
     * razón que AccountLookupDao.findLabel.
     *
     * Optional porque el empleado puede haberse dado de baja entre que se
     * eligió y que se consultó; quien llama decide qué enseñar entonces.
     */
    public java.util.Optional<String> findLabel(long cardholderId) {
        String sql = "SELECT first_name, last_name, employee_code FROM cardholder WHERE id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                return java.util.Optional.of(rs.getString("first_name") + " "
                        + rs.getString("last_name") + " · " + rs.getString("employee_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cardholder label", e);
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

    /**
     * UPDATE de la ficha. Recibe la conexión, no la abre: el correo también hay
     * que cambiarlo en app_user y las dos cosas son una sola operación. Quien
     * manda en la transacción es el servicio.
     *
     * employee_code y status NO están en el SET a propósito: el código no cambia
     * nunca y el estado tiene su propia operación (alta/baja).
     */
    public void update(Connection conn, Cardholder ch) throws SQLException {
        String sql = "UPDATE cardholder "
                + "   SET first_name = ?, last_name = ?, email = ?, "
                + "       phone = ?, department_id = ? "
                + " WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ch.getFirstName());
            ps.setString(2, ch.getLastName());
            ps.setString(3, ch.getEmail());
            ps.setString(4, ch.getPhone());
            setDepartment(ps, 5, ch.getDepartmentId());
            ps.setLong(6, ch.getId());
            ps.executeUpdate();
        }
    }

    /**
     * ¿Ese correo ya es de OTRO tarjetahabiente?
     *
     * emailExists(email) no sirve para editar: guardar a alguien sin cambiarle
     * el correo daría "ya está registrado" — contra sí mismo. De ahí el AND.
     */
    public boolean emailExistsForAnother(String email, long exceptId) {
        String sql = "SELECT 1 FROM cardholder WHERE UPPER(email) = UPPER(?) AND id <> ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking cardholder email", e);
        }
    }


    public void setStatus(long cardholderId, String status) {
        String sql = "UPDATE CARDHOLDER SET status = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, cardholderId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Cardholder " + cardholderId + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating the cardholder status", e);
        }
    }

}
