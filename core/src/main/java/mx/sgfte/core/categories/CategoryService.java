package mx.sgfte.core.categories;

import mx.sgfte.core.users.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules for the purpose catalogue.
 *
 * Kept apart from the DAO because the interesting part is not the INSERT: it is
 * what counts as a valid purpose, and the fact that purposes are retired rather
 * than deleted.
 */
public class CategoryService {

    /** The palette of the modal (2041:155), mirrored by --sgfte-purpose-N. */
    public static final int MIN_COLOR = 1;
    public static final int MAX_COLOR = 7;

    private static final int NAME_MAX = 40;
    private static final int DESCRIPTION_MAX = 120;

    private final CategoryDao dao;

    public CategoryService() { this(new CategoryDao()); }
    public CategoryService(CategoryDao dao) { this.dao = dao; }

    public List<CategoryAdminRow> catalogue() { return dao.findForAdmin(); }

    /**
     * Registers a purpose. Validates everything first and reports every problem
     * at once, so the admin does not fix one and discover the next on the
     * following attempt.
     */
    public long create(String name, String description, Integer colorIndex, boolean active) {
        Category category = validated(name, description, colorIndex);
        category.setStatus(active ? "ACTIVE" : "INACTIVE");
        try {
            return dao.create(category);
        } catch (DuplicateCategoryException e) {
            // El UNIQUE es quien decide; aquí sólo se traduce para la pantalla.
            throw new ValidationException(List.of(e.getMessage()));
        }
    }

    /**
     * Corrige una categoría existente.
     *
     * El color no es decorativo: es lo que distingue los propósitos en la tabla
     * de cuentas, en el selector de dispersión y en el dashboard, y sale de la
     * misma columna en los tres. Cambiarlo aquí los cambia todos a la vez, que
     * es justo lo que se quiere.
     */
    public void update(Long categoryId, String name, String description, Integer colorIndex) {
        if (categoryId == null) {
            throw new ValidationException(List.of("No se indicó qué categoría editar."));
        }
        dao.findById(categoryId).orElseThrow(
                () -> new ValidationException(List.of("La categoría ya no existe.")));

        Category category = validated(name, description, colorIndex);
        category.setId(categoryId);
        try {
            dao.update(category);
        } catch (DuplicateCategoryException e) {
            throw new ValidationException(List.of(e.getMessage()));
        }
    }

    /**
     * Valida y normaliza lo que llega del formulario, para el alta y para la
     * edición. Si el alta exige nombre y color de la paleta, la edición tiene
     * que exigir lo mismo: dos validadores para la misma entidad se separan en
     * cuanto alguien toca uno.
     */
    private Category validated(String name, String description, Integer colorIndex) {
        List<String> errors = new ArrayList<>();

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            errors.add("El nombre de la categoría es obligatorio.");
        } else if (cleanName.length() > NAME_MAX) {
            errors.add("El nombre no puede pasar de " + NAME_MAX + " caracteres.");
        }

        String cleanDescription = description == null ? null : description.trim();
        if (cleanDescription != null && cleanDescription.length() > DESCRIPTION_MAX) {
            errors.add("La descripción no puede pasar de " + DESCRIPTION_MAX + " caracteres.");
        }
        if (cleanDescription != null && cleanDescription.isEmpty()) cleanDescription = null;

        // El color llega de una paleta cerrada; fuera de rango es un formulario
        // manipulado, no un descuido, y el CHECK de la tabla lo rechazaría igual.
        if (colorIndex == null || colorIndex < MIN_COLOR || colorIndex > MAX_COLOR) {
            errors.add("Elige uno de los colores de la paleta.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);

        Category category = new Category();
        category.setName(cleanName);
        category.setDescription(cleanDescription);
        category.setColorIndex(colorIndex);
        return category;
    }

    /**
     * Retires a purpose or brings it back.
     *
     * Retirar no borra: account.category_id apunta aquí y es NOT NULL, así que
     * las cuentas que ya la usan conservan su propósito y su histórico; lo único
     * que cambia es que deja de ofrecerse al crear cuentas nuevas.
     */
    public boolean toggleStatus(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException(List.of("No se indicó qué categoría cambiar."));
        }
        Category current = dao.findById(categoryId).orElseThrow(
                () -> new ValidationException(List.of("La categoría ya no existe.")));

        boolean nowActive = !current.isActive();
        dao.setStatus(categoryId, nowActive ? "ACTIVE" : "INACTIVE");
        return nowActive;
    }
}
