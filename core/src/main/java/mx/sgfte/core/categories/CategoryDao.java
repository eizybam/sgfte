package mx.sgfte.core.categories;

import mx.sgfte.core.shared.db.Db;

import javax.naming.PartialResultException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    /**
     * SQL that yields a purpose's badge colour (1..7). Expects the category
     * table to be aliased as {@code cat}.
     *
     * It stays a constant even now that it is just a column. It was written
     * twice by hand once, and the two versions did not agree: one ranked with
     * DENSE_RANK over the query's OWN result set, so a filtered list coloured an
     * account differently than its detail page did. One name, one answer,
     * everywhere.
     *
     * Since V5 it reads a stored column instead of counting rows. The old form,
     *
     *     (SELECT MOD(COUNT(*), 4) + 1 FROM category c_rank WHERE c_rank.id < cat.id)
     *
     * derived the colour from the category's position in the catalogue, so
     * inserting one in the middle silently repainted every category after it —
     * and it gave the admin creating a category no say in the matter, which the
     * prototype's colour picker (2041:155) plainly expects.
     */
    public static final String PURPOSE_COLOR_SQL = "cat.color_index";

    // All active categories, ordered by name (for the dropdown)
    public  List<Category> findAllActive() {
        String sql = "SELECT id, name FROM category WHERE status = 'ACTIVE' ORDER BY name";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = Db.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(new Category(resultSet.getLong("id"), resultSet.getString("name")));
            }
            return categories;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading categories", e);
        }
    }

    /**
     * The whole catalogue for the admin screen (Figma 2036:6), active first and
     * then by name, with the two figures the table shows per row.
     *
     * Both totals are scalar subqueries and not joins: joining account and then
     * account_movement multiplies each account row by its movements, which would
     * inflate the count and the amount at once. Same trap as the cardholder
     * table, same fix.
     *
     * "Dispersión total" is what the Concentrator has put into that purpose over
     * all time, so it sums movements rather than the accounts' current balance —
     * money that arrived and was then spent still counts as dispersed.
     *
     * The type is DEPOSIT, not 'DISPERSION': that name only exists on the
     * Concentrator's side of the ledger. On the account's side the same transfer
     * lands as a DEPOSIT, which is what DispersionService writes and what
     * AccountDao.monthSummary already reads.
     *
     * TRANSFER_IN is deliberately left out. A P2P transfer only moves money
     * between accounts that already share this purpose, so counting it would add
     * up money the Concentrator never dispersed — and would count it twice,
     * since the deposit that funded it is already in the sum.
     */
    public List<CategoryAdminRow> findForAdmin() {
        String sql = """
                SELECT cat.id, cat.name, cat.description, cat.color_index, cat.status,
                       (SELECT COUNT(*) FROM account a
                         WHERE a.category_id = cat.id)            AS accounts,
                       (SELECT NVL(SUM(m.amount), 0)
                          FROM account_movement m
                          JOIN account a2 ON a2.id = m.account_id
                         WHERE a2.category_id = cat.id
                           AND m.movement_type = 'DEPOSIT')       AS dispersed
                  FROM category cat
                 ORDER BY CASE WHEN cat.status = 'ACTIVE' THEN 0 ELSE 1 END, cat.name
                """;
        List<CategoryAdminRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CategoryAdminRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("color_index"),
                        rs.getString("status"),
                        rs.getInt("accounts"),
                        rs.getBigDecimal("dispersed")));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the category catalogue", e);
        }
    }

    /**
     * Adds a purpose to the catalogue and returns its new id.
     *
     * The UNIQUE on name is what actually stops duplicates: checking first and
     * inserting after leaves a gap where two requests both pass the check. The
     * constraint violation is translated by the service into a message the admin
     * can read.
     */
    public long create(Category category) {
        String sql = "INSERT INTO category (name, description, color_index, status) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getColorIndex());
            ps.setString(4, category.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new DuplicateCategoryException(category.getName());
        } catch (SQLException e) {
            throw new RuntimeException("Error creating the category", e);
        }
    }

    /**
     * Corrige nombre, descripción o color de una categoría existente.
     *
     * status no está en el SET: retirar y reactivar es otra operación
     * (setStatus), con su propio evento en la bitácora. Si estuviera aquí, un
     * formulario de edición podría retirar una categoría sin que quedara
     * escrito como tal.
     *
     * El UNIQUE del nombre decide igual que en el alta; se traduce arriba.
     */
    public void update(Category category) {
        String sql = "UPDATE category SET name = ?, description = ?, color_index = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getColorIndex());
            ps.setLong(4, category.getId());
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Category " + category.getId() + " not found");
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new DuplicateCategoryException(category.getName());
        } catch (SQLException e) {
            throw new RuntimeException("Error updating the category", e);
        }
    }

    /**
     * Retires or brings back a purpose.
     *
     * There is no delete on purpose: account.category_id is NOT NULL and points
     * here, so removing a category in use would mean removing its accounts.
     * INACTIVE takes it out of the dropdowns —findAllActive already filters on
     * it— and leaves the history of the accounts that used it untouched.
     */
    public void setStatus(long categoryId, String status) {
        String sql = "UPDATE category SET status = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, categoryId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Category " + categoryId + " not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating the category status", e);
        }
    }

    /** One category by id, or empty. */
    public java.util.Optional<Category> findById(long id) {
        String sql = "SELECT id, name, description, color_index, status FROM category WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                return java.util.Optional.of(new Category(
                        rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                        rs.getInt("color_index"), rs.getString("status")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the category", e);
        }
    }
}
