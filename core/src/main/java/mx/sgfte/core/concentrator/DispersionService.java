package mx.sgfte.core.concentrator;

import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementDao;
import mx.sgfte.core.shared.db.Db;
import mx.sgfte.core.users.ValidationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dispersion: moves money from the Concentrator to a cardholder account.
 *
 * WARNING: this touches 3 things that must ALL happen or NONE:
 *   1) debit the Concentrator,
 *   2) credit the account,
 *   3) record the movement (DEPOSIT) in the ledger.
 * So it all runs in ONE transaction: autoCommit(false) -> commit() / rollback().
 * If step 2 or 3 fails, step 1 is undone and money never "disappears".
 */
public class DispersionService {

    private final ConcentratorDao concentratorDao;
    private final AccountDao accountDao;
    private final MovementDao movementDao;

    public DispersionService() {
        this(new ConcentratorDao(), new AccountDao(), new MovementDao());
    }

    public DispersionService(ConcentratorDao concentratorDao, AccountDao accountDao, MovementDao movementDao) {
        this.concentratorDao = concentratorDao;
        this.accountDao = accountDao;
        this.movementDao = movementDao;
    }

    /**
     * @param accountId   destination account
     * @param amount      amount in MXN (> 0)
     * @param description  optional ledger text
     */
    public void disperse(Long accountId, BigDecimal amount, String description) {
        List<String> errors = new ArrayList<>();
        if (accountId == null) errors.add("Debes elegir una cuenta destino");
        if (amount == null || amount.signum() <= 0) errors.add("El monto debe ser mayor a 0");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);            // transaction starts
            try {
                // 1) debit the Concentrator (false = no balance)
                if (!concentratorDao.debit(conn, amount)) {
                    throw new InsufficientFundsException("La Concentradora no tiene saldo suficiente");
                }
                // 2) credit the destination account (0 rows = missing or inactive account)
                if (!accountDao.credit(conn, accountId, amount)) {
                    throw new ValidationException(List.of("La cuenta destino no existe o está inactiva"));
                }
                // 3) record the DEPOSIT in the immutable ledger
                String text = (description == null || description.isBlank())
                        ? "Dispersión desde Concentradora" : description;
                movementDao.insert(conn, new Movement(accountId, "DEPOSIT", amount, null, text));

                conn.commit();                    // all good: confirm
            } catch (RuntimeException | SQLException e) {
                conn.rollback();                  // something failed: undo EVERYTHING
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error opening/closing the dispersion transaction", e);
        }
    }
}
