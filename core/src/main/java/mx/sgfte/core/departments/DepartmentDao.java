package mx.sgfte.core.departments;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD del catálogo de áreas. JDBC tonto: ni una regla, ni un mensaje para el
 * usuario — de eso se ocupa {@link DepartmentService}.
 */
public class DepartmentDao {

    /**
     * El catálogo con sus dos totales, para la tabla de administración.
     *
     * Los conteos van como subconsultas escalares y no como JOIN + GROUP BY
     * porque cardholder → account es uno a muchos: al unir, cada empleado se
     * repetiría una vez por cuenta y el conteo de empleados saldría inflado.
     *
     * Las activas primero y luego por nombre, igual que el catálogo de
     * categorías, para que retirar un área no la esconda: la baja al final.
     */
    public List<DepartmentAdminRow> findForAdmin() {
        String sql = """
                SELECT d.id, d.name, d.description, d.status,
                       (SELECT COUNT(*) FROM cardholder ch
                         WHERE ch.department_id = d.id
                           AND ch.status = 'ACTIVE')                AS employees,
                       (SELECT NVL(SUM(a.balance), 0)
                          FROM account a
                          JOIN cardholder ch2 ON ch2.id = a.cardholder_id
                         WHERE ch2.department_id = d.id
                           AND a.status = 'ACTIVE')                 AS funds
                  FROM department d
                 ORDER BY CASE WHEN d.status = 'ACTIVE' THEN 0 ELSE 1 END, d.name
                """;
        List<DepartmentAdminRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DepartmentAdminRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getInt("employees"),
                        rs.getBigDecimal("funds")));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the department catalogue", e);
        }
    }

    /** Sólo las activas: es lo que se ofrece al dar de alta o editar a alguien. */
    public List<Department> findAllActive() {
        String sql = "SELECT id, name, description, status FROM department "
                   + "WHERE status = 'ACTIVE' ORDER BY name";
        List<Department> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(map(rs));
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading active departments", e);
        }
    }

    public Optional<Department> findById(long id) {
        String sql = "SELECT id, name, description, status FROM department WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the department", e);
        }
    }

    /**
     * Alta. El UNIQUE del nombre es quien decide de verdad: comprobar antes e
     * insertar después deja un hueco por el que dos peticiones pasan las dos.
     */
    public long create(Department department) {
        String sql = "INSERT INTO department (name, description, status) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getDescription());
            ps.setString(3, department.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDepartmentException(department.getName());
        } catch (SQLException e) {
            throw new RuntimeException("Error creating the department", e);
        }
    }

    /**
     * Corrige nombre o descripción.
     *
     * status no está en el SET: retirar y reactivar es otra operación, con su
     * propio evento en la bitácora. Si estuviera aquí, un formulario de edición
     * podría retirar un área sin que quedara escrito como tal.
     */
    public void update(Department department) {
        String sql = "UPDATE department SET name = ?, description = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, department.getName());
            ps.setString(2, department.getDescription());
            ps.setLong(3, department.getId());
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Department " + department.getId() + " not found");
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDepartmentException(department.getName());
        } catch (SQLException e) {
            throw new RuntimeException("Error updating the department", e);
        }
    }

    /**
     * Retira un área o la devuelve al catálogo.
     *
     * No hay borrado: cardholder.department_id apunta aquí, así que borrar un
     * área con gente dentro significaría borrar a la gente. INACTIVE la saca de
     * los desplegables y deja intactos a los empleados que ya la tenían.
     */
    public void setStatus(long departmentId, String status) {
        String sql = "UPDATE department SET status = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, departmentId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Department " + departmentId + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating the department status", e);
        }
    }

    private Department map(ResultSet rs) throws SQLException {
        return new Department(rs.getLong("id"), rs.getString("name"),
                              rs.getString("description"), rs.getString("status"));
    }
}
