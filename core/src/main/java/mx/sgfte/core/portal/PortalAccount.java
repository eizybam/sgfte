package mx.sgfte.core.portal;

import java.math.BigDecimal;

/**
 * Read model for one of the employee's own accounts.
 *
 * Deliberately not the {@code Account} entity: the portal needs the purpose
 * NAME (not the category id) and the active card count, and it must never be
 * able to write. Keeping a separate read model means a screen can't accidentally
 * be handed a mutable entity.
 */
public class PortalAccount {

    private final long id;
    private final String accountNumber;   // public code, e.g. "GAS-48HSY"
    private final long categoryId;
    private final String purpose;         // category name, e.g. "Gasolina"
    private final BigDecimal balance;
    private final int activeCards;

    public PortalAccount(long id, String accountNumber, long categoryId,
                         String purpose, BigDecimal balance, int activeCards) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.categoryId = categoryId;
        this.purpose = purpose;
        this.balance = balance;
        this.activeCards = activeCards;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public long getCategoryId() { return categoryId; }
    public String getPurpose() { return purpose; }
    public BigDecimal getBalance() { return balance; }
    public int getActiveCards() { return activeCards; }
}
