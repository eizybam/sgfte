package mx.sgfte.core.funding;

import mx.sgfte.core.concentrator.ConcentratorDao;
import mx.sgfte.core.shared.db.Db;
import mx.sgfte.core.users.ValidationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fondeo de la Concentradora a partir de un depósito bancario (V12).
 *
 * Sustituye al viejo ConcentratorService.fund(monto), donde un administrador
 * tecleaba una cifra y el saldo subía. Aquí no se puede fondear sin depósito, y
 * un depósito no se acepta sin referencia bancaria: la única puerta por la que
 * entra dinero al sistema pasa por este método.
 *
 * Regla de oro del proyecto: el dinero se mueve dentro de un Service, en UNA
 * transacción. Aquí son tres escrituras —el depósito, el saldo y el asiento del
 * ledger— y las tres caen juntas o no cae ninguna. Si el asiento se pudiera
 * escribir sin el depósito volveríamos justo al problema que V12 vino a
 * resolver: un fondeo sin respaldo.
 */
public class FundingService {

    private final FundingDepositDao deposits;
    private final ConcentratorDao concentrator;

    public FundingService() {
        this(new FundingDepositDao(), new ConcentratorDao());
    }

    public FundingService(FundingDepositDao deposits, ConcentratorDao concentrator) {
        this.deposits = deposits;
        this.concentrator = concentrator;
    }

    /**
     * Registra un depósito y abona la Concentradora.
     *
     * @return el depósito con su id ya asignado.
     * @throws ValidationException        si algo del depósito no cuadra.
     * @throws DuplicateDepositException  si esa referencia ya se contabilizó.
     */
    public FundingDeposit register(FundingDeposit d, String beneficiarioEsperado) {
        validate(d, beneficiarioEsperado);

        /*
          Se consulta antes para poder dar un mensaje claro, pero la garantía es
          el UNIQUE: entre esta consulta y el INSERT cabe otra petición con la
          misma referencia. Por eso abajo se atrapa también la violación del
          índice. Comprobar y confiar sería un TOCTOU sobre el dinero.
         */
        if (deposits.exists(d.getReferencia())) {
            throw new DuplicateDepositException(d.getReferencia());
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                long depositId = deposits.insert(c, d);
                concentrator.creditFromDeposit(c, d.getMonto(), d.getOrdenanteNombre(), depositId);
                c.commit();
                d.setId(depositId);
                return d;
            } catch (SQLIntegrityConstraintViolationException e) {
                c.rollback();
                // Carrera perdida contra otra petición con la misma referencia,
                // o un CHECK del canal que el validate no cubrió.
                if (isDuplicateReference(e)) throw new DuplicateDepositException(d.getReferencia());
                throw new ValidationException(List.of(
                        "El depósito no cumple las reglas del canal " + d.getCanal()));
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando el depósito", e);
        }
    }

    /**
     * Las reglas del depósito, todas juntas para que la vista pueda enseñarlas
     * de una vez en vez de una por intento.
     *
     * Las del canal se repiten en CHECK constraints a propósito: aquí para
     * poder explicarlas en español, y en la base para que ninguna otra ruta
     * —un script, otro servicio— pueda saltárselas.
     */
    private void validate(FundingDeposit d, String beneficiarioEsperado) {
        List<String> errors = new ArrayList<>();

        if (blank(d.getReferencia())) {
            errors.add("La referencia del depósito es obligatoria");
        }
        if (blank(d.getOrdenanteNombre())) {
            errors.add("La razón social de quien deposita es obligatoria");
        }
        if (blank(d.getInstitucion())) {
            errors.add("La institución bancaria es obligatoria");
        }
        if (d.getMonto() == null || d.getMonto().signum() <= 0) {
            errors.add("El monto debe ser mayor a 0");
        }
        if (d.getFechaOperacion() == null) {
            errors.add("La fecha de la operación es obligatoria");
        } else if (d.getFechaOperacion().isAfter(LocalDateTime.now().plusMinutes(5))) {
            // Cinco minutos de gracia por el reloj del banco; más que eso es un
            // depósito que todavía no ocurre.
            errors.add("La fecha de la operación no puede estar en el futuro");
        }

        // El destino tiene que ser nuestra cuenta. Un depósito a otra CLABE no
        // es dinero nuestro por más que alguien lo capture aquí.
        String beneficiario = Clabe.normalize(d.getBeneficiarioClabe());
        if (beneficiario == null || !Clabe.isValid(beneficiario)) {
            errors.add("La CLABE beneficiaria no es válida");
        } else if (beneficiarioEsperado != null
                && !beneficiario.equals(Clabe.normalize(beneficiarioEsperado))) {
            errors.add("El depósito no va dirigido a la CLABE de la Concentradora");
        } else {
            d.setBeneficiarioClabe(beneficiario);
        }

        if (FundingDeposit.SPEI.equals(d.getCanal())) {
            validateSpei(d, errors);
        } else if (FundingDeposit.VENTANILLA.equals(d.getCanal())) {
            validateVentanilla(d, errors);
        } else {
            errors.add("Canal de depósito desconocido: " + d.getCanal());
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    /** Un SPEI siempre viene de una cuenta, y esa cuenta tiene que ser real. */
    private void validateSpei(FundingDeposit d, List<String> errors) {
        String ordenante = Clabe.normalize(d.getOrdenanteClabe());
        if (ordenante == null || !Clabe.isValid(ordenante)) {
            errors.add("La CLABE ordenante no es válida");
        } else {
            d.setOrdenanteClabe(ordenante);
        }
        if (!blank(d.getSucursal())) {
            errors.add("Una transferencia SPEI no tiene sucursal");
        }
    }

    /**
     * En ventanilla no hay cuenta ordenante que identifique a nadie, así que el
     * RFC deja de ser opcional. Es la compensación por usar el canal con menos
     * rastro: menos trazabilidad, más identificación.
     */
    private void validateVentanilla(FundingDeposit d, List<String> errors) {
        if (!blank(d.getOrdenanteClabe())) {
            errors.add("Un depósito en efectivo no tiene CLABE ordenante");
        }
        if (blank(d.getOrdenanteRfc())) {
            errors.add("En ventanilla el RFC de quien deposita es obligatorio: "
                     + "sin cuenta ordenante es lo único que identifica el origen");
        }
        if (blank(d.getSucursal())) {
            errors.add("La sucursal donde se hizo el depósito es obligatoria");
        }
    }

    /** ORA-00001 es violación de índice único; el nombre dice cuál. */
    private boolean isDuplicateReference(SQLIntegrityConstraintViolationException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toUpperCase();
        return message.contains("UQ_FUNDING_DEPOSIT_REF");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
