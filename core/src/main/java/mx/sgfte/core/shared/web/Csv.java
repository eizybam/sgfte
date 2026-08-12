package mx.sgfte.core.shared.web;

public final class Csv {

    /*
      Excel en configuración regional española/mexicana espera ';' como
      separador y lee un archivo con ',' como una sola columna. Es un cambio de
      un carácter, aquí, si en la defensa se abre en un Excel en español:
      cambiar DELIM a ';'.
    */
    private static final char DELIM = ',';

    /**
     * RFC 4180 pide CRLF; Excel lo agradece y el resto lo tolera.
     */
    private static final String EOL = "\r\n";

    private final StringBuilder out = new StringBuilder();
    private int rows;

    /**
     * Un renglón. Los null salen como celda vacía.
     */
    public Csv row(Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) out.append(DELIM);
            out.append(cell(cells[i]));
        }
        out.append(EOL);
        rows++;
        return this;
    }

    /**
     * Renglón en blanco: separa secciones dentro de un mismo archivo.
     */
    public Csv blank() {
        out.append(EOL);
        return this;
    }

    public int getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    private String cell(Object value) {
        if (value == null) return "";
        /*
          Los números salen crudos, sin comillas y sin el apóstrofo de guard():
          un "-1500.00" protegido como texto deja de ser un número en la hoja de
          cálculo, y entonces el reporte no se puede sumar. Por eso el filtro de
          fórmulas se aplica sólo a las celdas de texto.
        */
        if (value instanceof Number) return value.toString();
        return quote(guard(value.toString()));
    }
    /**
     * Neutraliza la inyección de fórmulas (CSV injection).
     *
     * Una celda que empieza con =, +, - o @ la ejecuta Excel como fórmula al
     * abrir el archivo. En esta bitácora el campo `detail` y el campo `actor`
     * los escribe, indirectamente, quien usa el sistema: basta con darse de alta
     * con un nombre que empiece con '=' para colar una fórmula en el reporte que
     * abrirá un administrador. El apóstrofo inicial le dice a Excel "esto es
     * texto" y no se muestra en la celda.
     */
    private String guard(String text) {
        if (text.isEmpty()) return text;
        char first = text.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + text;
        }
        return text;
    }

    /** Comillas sólo cuando hacen falta; las comillas internas se duplican. */
    private String quote(String text) {
        boolean needed = text.indexOf(DELIM) >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (!needed) return text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
