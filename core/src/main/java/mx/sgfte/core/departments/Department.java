package mx.sgfte.core.departments;

/**
 * Un área de la empresa. Catálogo, no entidad de negocio: no guarda dinero ni
 * gente — es la lista de la que tira el desplegable "Departamento" del alta y
 * de la edición de un tarjetahabiente.
 *
 * Gemelo de {@link mx.sgfte.core.categories.Category} salvo por el color: una
 * categoría se pinta en tablas y gráficas, un departamento no se pinta en
 * ninguna parte.
 */
public class Department {

    private Long id;
    private String name;
    private String description;
    private String status = "ACTIVE";

    public Department() {}

    public Department(Long id, String name, String description, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "ACTIVE".equals(status); }
}
