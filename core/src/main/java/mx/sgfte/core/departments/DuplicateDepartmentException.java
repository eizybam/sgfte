package mx.sgfte.core.departments;

/** El UNIQUE de department.name lo rechazó. El servicio la traduce a mensaje. */
public class DuplicateDepartmentException extends RuntimeException {

    public DuplicateDepartmentException(String name) {
        super("Ya existe un departamento con el nombre \"" + name + "\".");
    }
}
