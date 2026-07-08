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
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if  (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (amount.compareTo(this.balance) > 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        this.balance = this.balance.subtract(amount);
    }

    public String getId()          { return id; }
    public String getPurpose()     { return purpose; }
    public BigDecimal getBalance() { return balance; }
}