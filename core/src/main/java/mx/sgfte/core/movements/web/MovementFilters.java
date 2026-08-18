package mx.sgfte.core.movements.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Los seis filtros de /admin/movimientos, leídos en UN solo sitio.
 *
 * <p>Existe por un fallo concreto que hay en la exportación de la bitácora y que
 * conviene no repetir: allí la pantalla lee {@code q}, {@code sev} y {@code mod},
 * el enlace de descarga manda {@code q}, {@code sev} y {@code moduleFilter}, y
 * el servlet del CSV lee {@code search}, {@code severity} y {@code moduleFilter}.
 * Dos de los tres filtros no llegan. El archivo sale con TODO aunque en pantalla
 * se esté viendo sólo lo crítico, y no avisa: parece que funciona.
 *
 * <p>Nada de eso puede pasar aquí porque la pantalla y el CSV no leen la
 * petición cada uno por su lado — los dos llaman a {@link #from}. Si un filtro
 * cambia de nombre, cambia para los dos a la vez o no compila.
 *
 * <p>Es un record y no una clase con getters porque no llega a ningún JSP: el
 * servlet reparte los valores como atributos sueltos, así que EL nunca ve este
 * objeto y la trampa de los accesores no aplica.
 */
record MovementFilters(String search, String scope, String type,
                       Long categoryId, Long accountId, String period) {

    /** Lee los filtros de la petición. Único sitio donde se nombran. */
    static MovementFilters from(HttpServletRequest req) {
        return new MovementFilters(
                trimToNull(req.getParameter("q")),
                normalizeScope(req.getParameter("ambito")),
                trimToNull(req.getParameter("tipo")),
                parseId(req.getParameter("cat")),
                parseId(req.getParameter("cuenta")),
                normalizePeriod(req.getParameter("period")));
    }

    /** ¿Hay algún filtro puesto? Lo usa el CSV para describirse a sí mismo. */
    boolean any() {
        return search != null || scope != null || type != null
            || categoryId != null || accountId != null || !"TODOS".equals(period);
    }

    /**
     * Sólo los dos ámbitos reales filtran; cualquier otra cosa es "todos".
     *
     * Se acepta el alias corto CONC porque es lo que escriben los enlaces de la
     * Concentradora: una URL que puede acabar tecleada en una defensa se
     * agradece corta.
     */
    private static String normalizeScope(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase();
        if ("CONC".equals(value) || "CONCENTRADORA".equals(value)) return "CONCENTRADORA";
        if ("CUENTA".equals(value) || "CUENTAS".equals(value))     return "CUENTA";
        return null;
    }

    /**
     * TODOS es el filtro de fecha apagado, igual que en el portal: una base
     * recién sembrada no tiene movimientos de hoy, y abrir el historial en una
     * tabla vacía parece que está roto.
     */
    private static String normalizePeriod(String raw) {
        if ("HOY".equals(raw) || "7D".equals(raw) || "30D".equals(raw)) return raw;
        return "TODOS";
    }

    /*
      `tipo` no se valida contra una lista blanca y no hace falta: viaja como
      parámetro enlazado, así que un tipo inventado devuelve cero filas, no un
      error ni una inyección. El desplegable se llena con distinctTypes().
     */
    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static String trimToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
