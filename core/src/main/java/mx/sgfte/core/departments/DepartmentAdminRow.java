package mx.sgfte.core.departments;

import java.math.BigDecimal;

/**
 * Una fila de la tabla "Departamentos" de /admin/categorias.
 *
 * Mismas columnas que el catálogo de categorías salvo el color: cuántos
 * empleados tiene el área y cuánto suman sus cuentas activas. Las dos
 * responden a la pregunta que se hace quien mira el catálogo — "¿esta área
 * está en uso?"— antes de retirarla.
 */
public record DepartmentAdminRow(
        long id,
        String name,
        String description,
        String status,
        int employees,
        BigDecimal funds) {

    public boolean isActive() { return "ACTIVE".equals(status); }

    /*
      Getters JavaBean: Tomcat 10.1 trae EL 5.0, que resuelve ${x.name} por
      getName() y no ve los accesores de un record. Sin esto la página revienta
      a media renderización y la respuesta llega truncada, no con un 500.
     */
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public int getEmployees() { return employees; }
    public BigDecimal getFunds() { return funds; }
    public boolean getActive() { return isActive(); }
}
