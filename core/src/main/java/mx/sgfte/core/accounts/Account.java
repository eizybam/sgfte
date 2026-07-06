package mx.sgfte.core.accounts;
import java.math.BigDecimal;

public class Account {
    private final String id;
    private final String purpose;
    private BigDecimal balance;

    public Account(String id, String purpose) {
        this.id = id;
        this.purpose = purpose;
        this.balance = BigDecimal.ZERO;
    }

    public void deposit(BigDecimal amount) {
        if  (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        balance = balance.add(amount);
    }
}