package mx.sgfte.core.accounts;

import java.math.BigDecimal;

/**
 * An account with a specific purpose (category). MONEY LIVES HERE.
 * - id: internal numeric PK (what FKs and joins use; never shown to users).
 * - accountNumber: public business code shown to users, e.g. "GAS-48HSY".
 */
public class Account {
    private Long id;
    private String accountNumber;
    private Long cardholderId;
    private final Long categoryId;
    private BigDecimal balance;
    private boolean isActive;

    public Account(Long cardholderId, Long categoryId) {
        this.cardholderId = cardholderId;
        this.categoryId = categoryId;
        this.balance = BigDecimal.ZERO;
        this.isActive = true;
    }

    public void deposit(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (amount.compareTo(this.balance) > 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        this.balance = this.balance.subtract(amount);
    }

    public Long getId()                { return id; }
    public void setId(Long id)         { this.id = id; }

    public String getAccountNumber()                    { return accountNumber; }
    public void setAccountNumber(String accountNumber)  { this.accountNumber = accountNumber; }

    public Long getCardholderId()      { return cardholderId; }
    public Long getCategoryId()        { return categoryId; }

    public BigDecimal getBalance()             { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public boolean isActive()              { return isActive; }
    public void setActive(boolean active)  { this.isActive = active; }
}
