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
     * SQL that yields a purpose's badge colour (1..4): its position in the
     * catalogue ordered by id, wrapped at four. Expects the category table to be
     * aliased as {@code cat}.
     *
     * It lives here, as one string, because it was written twice by hand and the
     * two versions did not agree: one ranked with DENSE_RANK over the query's
     * OWN result set, so a filtered list coloured an account differently than
     * its detail page did. Ranking over the whole catalogue is the stable
     * answer — it does not depend on which rows a query happens to return.
     *
     * It also avoids a window function, which Oracle refuses to nest inside an
     * aggregate (ORA-30483) when the query groups.
     */
    public static final String PURPOSE_COLOR_SQL =
            "(SELECT MOD(COUNT(*), 4) + 1 FROM category c_rank WHERE c_rank.id < cat.id)";

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
}
