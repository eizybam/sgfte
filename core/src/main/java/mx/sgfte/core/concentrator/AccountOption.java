package mx.sgfte.core.concentrator;

import java.math.BigDecimal;

/**
 * Lightweight read-only DTO for account select dropdowns.
 * Properties exposed as JavaBean getters so JSP EL can read ${a.id}, ${a.label}, ${a.balance}.
 */
public class AccountOption {
    private final long id;
    private final String label;
    private final BigDecimal balance;

    public AccountOption(long id, String label, BigDecimal balance) {
        this.id = id;
        this.label = label;
        this.balance = balance;
    }

    public long getId() { return id; }
    public String getLabel() { return label; }
    public BigDecimal getBalance() { return balance; }
}
