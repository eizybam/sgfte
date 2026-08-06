package mx.sgfte.core.categories;

/**
 * The catalogue already has a purpose with that name.
 *
 * Raised from the UNIQUE violation rather than from a prior SELECT: checking
 * first and inserting after leaves a window where two requests both pass the
 * check and one still fails at the constraint.
 */
public class DuplicateCategoryException extends RuntimeException {
    private final String name;

    public DuplicateCategoryException(String name) {
        super("Ya existe una categoría llamada \"" + name + "\".");
        this.name = name;
    }

    public String getName() { return name; }
}
