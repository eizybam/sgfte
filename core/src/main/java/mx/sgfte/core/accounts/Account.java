package mx.sgfte.core.accounts;
import java.math.BigDecimal;

public class Account {
    private final String id;
    private final Long cardholderId;
    private final Long categoryId;
    private BigDecimal balance;
    private boolean isActive;

    public Account(String id, Long cardholderId, Long categoryId) {
        this.id = id;
        this.cardholderId = cardholderId;
        this.categoryId = categoryId;
        this.balance = BigDecimal.ZERO;
        this.isActive = true;

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
    public Long getCategoryId()     { return categoryId; }
    public BigDecimal getBalance() { return balance; }
    public String getCardholderId() {return cardholderId;}
}