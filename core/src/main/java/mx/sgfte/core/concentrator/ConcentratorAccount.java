package mx.sgfte.core.concentrator;

import java.math.BigDecimal;

public class ConcentratorAccount {
    private long  id;
    private String name;
    private BigDecimal balance;

    public ConcentratorAccount() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
