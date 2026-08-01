package mx.sgfte.core.concentrator;

import mx.sgfte.core.users.ValidationException;

import java.math.BigDecimal;
import java.util.List;

/** Concentrator logic: read the balance and fund it (put company money in). */
public class ConcentratorService {

    private final ConcentratorDao dao;

    public ConcentratorService() {
        this(new ConcentratorDao());
    }

    public ConcentratorService(ConcentratorDao dao) {
        this.dao = dao;
    }

    public ConcentratorAccount getConcentrator() {
        return dao.findSingleton();
    }

    /** Adds money to the Concentrator. Validates the amount is positive. */
    public void fund(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException(List.of("El monto a fondear debe ser mayor a 0"));
        }
        dao.fund(amount);
    }
}
