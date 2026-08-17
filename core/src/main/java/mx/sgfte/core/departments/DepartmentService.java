package mx.sgfte.core.departments;

import mx.sgfte.core.users.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del catálogo de áreas (RF-02).
 *
 * Calcado de {@link mx.sgfte.core.categories.CategoryService} porque el
 * problema es el mismo; lo que cambia es que aquí no hay paleta de colores que
 * validar.
 */
public class DepartmentService {

    public static final int NAME_MAX = 60;
    public static final int DESCRIPTION_MAX = 120;

    private final DepartmentDao dao;

    public DepartmentService() { this(new DepartmentDao()); }
    public DepartmentService(DepartmentDao dao) { this.dao = dao; }

    public List<DepartmentAdminRow> catalogue() { return dao.findForAdmin(); }

    /** Las que se pueden elegir hoy al dar de alta o editar a un empleado. */
    public List<Department> selectable() { return dao.findAllActive(); }

    public long create(String name, String description, boolean active) {
        Department department = validated(name, description);
        department.setStatus(active ? "ACTIVE" : "INACTIVE");
        try {
            return dao.create(department);
        } catch (DuplicateDepartmentException e) {
            throw new ValidationException(List.of(e.getMessage()));
        }
    }

    public void update(Long departmentId, String name, String description) {
        if (departmentId == null) {
            throw new ValidationException(List.of("No se indicó qué departamento editar."));
        }
        dao.findById(departmentId).orElseThrow(
                () -> new ValidationException(List.of("El departamento ya no existe.")));

        Department department = validated(name, description);
        department.setId(departmentId);
        try {
            dao.update(department);
        } catch (DuplicateDepartmentException e) {
            throw new ValidationException(List.of(e.getMessage()));
        }
    }

    /**
     * Retira un área o la reactiva. Devuelve su estado NUEVO.
     *
     * Retirar no toca a nadie: quien ya la tenía asignada la conserva —su ficha
     * la sigue enseñando— y lo único que cambia es que deja de ofrecerse en los
     * desplegables. Por eso no hace falta comprobar si está vacía.
     */
    public boolean toggleStatus(Long departmentId) {
        if (departmentId == null) {
            throw new ValidationException(List.of("No se indicó qué departamento cambiar."));
        }
        Department current = dao.findById(departmentId).orElseThrow(
                () -> new ValidationException(List.of("El departamento ya no existe.")));

        boolean nowActive = !current.isActive();
        dao.setStatus(departmentId, nowActive ? "ACTIVE" : "INACTIVE");
        return nowActive;
    }

    /**
     * Valida y normaliza lo que llega del formulario, para el alta y para la
     * edición. Reúne todos los problemas y los reporta juntos: nadie quiere
     * arreglar uno y descubrir el siguiente al enviar.
     */
    private Department validated(String name, String description) {
        List<String> errors = new ArrayList<>();

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            errors.add("El nombre del departamento es obligatorio.");
        } else if (cleanName.length() > NAME_MAX) {
            errors.add("El nombre no puede pasar de " + NAME_MAX + " caracteres.");
        }

        String cleanDescription = description == null ? null : description.trim();
        if (cleanDescription != null && cleanDescription.length() > DESCRIPTION_MAX) {
            errors.add("La descripción no puede pasar de " + DESCRIPTION_MAX + " caracteres.");
        }
        if (cleanDescription != null && cleanDescription.isEmpty()) cleanDescription = null;

        if (!errors.isEmpty()) throw new ValidationException(errors);

        Department department = new Department();
        department.setName(cleanName);
        department.setDescription(cleanDescription);
        return department;
    }
}
