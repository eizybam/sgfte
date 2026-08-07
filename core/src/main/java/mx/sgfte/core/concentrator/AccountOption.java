package mx.sgfte.core.concentrator;

import java.math.BigDecimal;

/** Lightweight read model for the dispersion account dropdown. */
public class AccountOption {
    private final long id;
    private final String label;      // e.g. "López, Ana — Gasolina"
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
