package mx.sgfte.core.accounts;

import java.math.BigDecimal;

/** Header and balance block of the "Detalle de Cuenta" screen (Figma 2043:155). */
public class AccountDetail {

    private final long id;
    private final String accountNumber;
    private final String purpose;
    private final int purposeColor;   // 1..4, igual criterio que el listado
    private final String holderName;
    private final BigDecimal balance;
    private final String status;

    public AccountDetail(long id, String accountNumber, String purpose, int purposeColor,
                         String holderName, BigDecimal balance, String status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.purpose = purpose;
        this.purposeColor = purposeColor;
        this.holderName = holderName;
        this.balance = balance;
        this.status = status;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getPurpose() { return purpose; }
    public int getPurposeColor() { return purposeColor; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
