package mx.sgfte.core.transfers;

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
 * P2P transfer between two accounts of the SAME purpose (category).
 *
 * Same transactional pattern as DispersionService (Module 1):
 *   debit source + credit destination + 2 ledger rows (TRANSFER_OUT / TRANSFER_IN)
 * all in ONE transaction. If any step fails, everything rolls back.
 *
 * Business rule 5: only allowed if both accounts share the exact same category_id.
 */
public class TransferService {

    private final TransferDao transferDao;
    private final AccountDao accountDao;
    private final MovementDao movementDao;

    public TransferService() {
        this(new TransferDao(), new AccountDao(), new MovementDao());
    }

    public TransferService(TransferDao transferDao, AccountDao accountDao, MovementDao movementDao) {
        this.transferDao = transferDao;
        this.accountDao = accountDao;
        this.movementDao = movementDao;
    }

    public void transfer(Long sourceId, Long destId, BigDecimal amount, String description) {
        List<String> errors = new ArrayList<>();
        if (sourceId == null) errors.add("Elige la cuenta origen");
        if (destId == null) errors.add("Elige la cuenta destino");
        if (sourceId != null && sourceId.equals(destId)) errors.add("Origen y destino no pueden ser la misma cuenta");
        if (amount == null || amount.signum() <= 0) errors.add("El monto debe ser mayor a 0");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long catSource = transferDao.categoryIdIfActive(conn, sourceId);
                Long catDest = transferDao.categoryIdIfActive(conn, destId);
                if (catSource == null || catDest == null) {
                    throw new ValidationException(List.of("Alguna de las cuentas no existe o está inactiva"));
                }
                // Business rule 5: same purpose only
                if (!catSource.equals(catDest)) {
                    throw new ValidationException(List.of("Solo se permite transferir entre cuentas del mismo propósito"));
                }
                // debit source (false = not enough balance)
                if (!accountDao.debit(conn, sourceId, amount)) {
                    throw new ValidationException(List.of("Saldo insuficiente en la cuenta origen"));
                }
                accountDao.credit(conn, destId, amount);

                String text = (description == null || description.isBlank()) ? "Transferencia P2P" : description;
                movementDao.insert(conn, new Movement(sourceId, "TRANSFER_OUT", amount, destId, text));
                movementDao.insert(conn, new Movement(destId, "TRANSFER_IN", amount, sourceId, text));

                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error opening/closing the transfer transaction", e);
        }
    }
}
