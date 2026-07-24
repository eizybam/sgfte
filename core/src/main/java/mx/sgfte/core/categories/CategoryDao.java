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
