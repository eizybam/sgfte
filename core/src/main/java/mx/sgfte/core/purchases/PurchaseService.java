package mx.sgfte.core.purchases;

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
 * Simulates a real-life card purchase: a WITHDRAWAL against one account.
 *
 * account_movement already modeled WITHDRAWAL — the portal's own movement
 * history even had a label ready for it ("Consumo con tarjeta") — but nothing
 * in the app ever wrote one. "Hacer un gasto" is that missing writer.
 *
 * Same transactional shape as TransferService/DispersionService: debit +
 * ledger row, one transaction, rolls back together. There is no credit side
 * and no counterparty here — a purchase leaves the system, it does not move
 * to another account — so this is the smallest version of that pattern, not
 * a special case bolted onto TransferService.
 */
public class PurchaseService {

    private final AccountDao accountDao;
    private final MovementDao movementDao;

    public PurchaseService() {
        this(new AccountDao(), new MovementDao());
    }

    public PurchaseService(AccountDao accountDao, MovementDao movementDao) {
        this.accountDao = accountDao;
        this.movementDao = movementDao;
    }

    public void spend(long accountId, BigDecimal amount, String merchant) {
        List<String> errors = new ArrayList<>();
        if (amount == null || amount.signum() <= 0) errors.add("El monto debe ser mayor a 0");
        if (merchant == null || merchant.isBlank()) errors.add("Escribe dónde se hizo la compra");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // debit() es la misma comprobación atómica que ya usan
                // transferencias y dispersión: sólo descuenta si SÍ alcanza,
                // así que no hace falta un SELECT de saldo aparte que otra
                // conexión concurrente pudiera dejar obsoleto.
                if (!accountDao.debit(conn, accountId, amount)) {
                    throw new ValidationException(List.of("Saldo insuficiente en la cuenta"));
                }
                movementDao.insert(conn, new Movement(accountId, "WITHDRAWAL", amount, null, merchant.trim()));

                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error opening/closing the purchase transaction", e);
        }
    }
}
