package mx.sgfte.core.funding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Un depósito recibido: el respaldo bancario de un fondeo (V12).
 *
 * Esto NO es un movimiento del ledger — el movimiento lo escribe
 * ConcentratorDao y apunta aquí. Esto es la evidencia de que el dinero existió
 * antes de entrar al sistema: quién lo mandó, por qué canal, con qué
 * referencia y cuándo lo procesó el banco.
 *
 * Clase con getters JavaBean y no record: llega a un JSP y EL 5.0 no ve los
 * accesores de un record (ver CategoryAdminRow).
 */
public class FundingDeposit {

    /** Transferencia electrónica: trae clave de rastreo y cuenta ordenante. */
    public static final String SPEI = "SPEI";
    /** Depósito en efectivo en sucursal: trae folio de ficha, no cuenta. */
    public static final String VENTANILLA = "VENTANILLA";

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY  = DateTimeFormatter.ofPattern("dd/MMM/yyyy", ES_MX);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private Long id;
    private String canal;
    private String referencia;
    private String ordenanteNombre;
    private String ordenanteRfc;
    private String ordenanteClabe;
    private String institucion;
    private String sucursal;
    private String beneficiarioClabe;
    private BigDecimal monto;
    private String concepto;
    private Integer referenciaNumerica;
    private LocalDateTime fechaOperacion;
    private LocalDateTime recibidoEn;

    public FundingDeposit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getOrdenanteNombre() { return ordenanteNombre; }
    public void setOrdenanteNombre(String ordenanteNombre) { this.ordenanteNombre = ordenanteNombre; }

    public String getOrdenanteRfc() { return ordenanteRfc; }
    public void setOrdenanteRfc(String ordenanteRfc) { this.ordenanteRfc = ordenanteRfc; }

    public String getOrdenanteClabe() { return ordenanteClabe; }
    public void setOrdenanteClabe(String ordenanteClabe) { this.ordenanteClabe = ordenanteClabe; }

    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }

    public String getSucursal() { return sucursal; }
    public void setSucursal(String sucursal) { this.sucursal = sucursal; }

    public String getBeneficiarioClabe() { return beneficiarioClabe; }
    public void setBeneficiarioClabe(String beneficiarioClabe) { this.beneficiarioClabe = beneficiarioClabe; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public Integer getReferenciaNumerica() { return referenciaNumerica; }
    public void setReferenciaNumerica(Integer referenciaNumerica) { this.referenciaNumerica = referenciaNumerica; }

    public LocalDateTime getFechaOperacion() { return fechaOperacion; }
    public void setFechaOperacion(LocalDateTime fechaOperacion) { this.fechaOperacion = fechaOperacion; }

    public LocalDateTime getRecibidoEn() { return recibidoEn; }
    public void setRecibidoEn(LocalDateTime recibidoEn) { this.recibidoEn = recibidoEn; }

    // ---- Etiquetas para la vista -------------------------------------------

    public boolean isSpei() { return SPEI.equals(canal); }

    /** Cómo se llama la referencia según el canal: no es lo mismo un folio que una clave. */
    public String getReferenciaLabel() {
        return isSpei() ? "Clave de rastreo" : "Folio de depósito";
    }

    public String getCanalLabel() {
        return isSpei() ? "SPEI" : "Efectivo en ventanilla";
    }

    /**
     * De dónde salió, en una línea.
     *
     * En SPEI la cuenta ordenante identifica el origen; en ventanilla no hay
     * cuenta, así que lo identifica la sucursal donde se paró el depositante.
     */
    public String getOrigenLabel() {
        if (isSpei()) return institucion + " · " + Clabe.format(ordenanteClabe);
        return institucion + (sucursal == null || sucursal.isBlank() ? "" : " · Suc. " + sucursal);
    }

    public String getDayLabel() {
        if (fechaOperacion == null) return "";
        String day = DAY.format(fechaOperacion);
        int slash = day.indexOf('/');
        if (slash < 0 || slash + 1 >= day.length()) return day;
        return day.substring(0, slash + 1)
             + Character.toUpperCase(day.charAt(slash + 1))
             + day.substring(slash + 2);
    }

    public String getTimeLabel() {
        return fechaOperacion == null ? "" : TIME.format(fechaOperacion);
    }
}
