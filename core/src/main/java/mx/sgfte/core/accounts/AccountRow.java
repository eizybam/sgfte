package mx.sgfte.core.accounts;

/**
 * One line of the "Gestión de Cuentas" table (Figma 225:41): the account code,
 * who it belongs to, what it is for, how many live cards hang off it and
 * whether it is still active.
 *
 * It is a read model for that screen only — it deliberately carries no balance,
 * because the frame does not show one.
 */
public class AccountRow {

    private final long id;
    private final String accountNumber;
    private final String holderName;
    private final String purpose;
    private final int purposeColor;   // 1..4, elige el color del badge
    private final int activeCards;
    private final String status;      // ACTIVE | INACTIVE

    public AccountRow(long id, String accountNumber, String holderName, String purpose,
                      int purposeColor, int activeCards, String status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.purpose = purpose;
        this.purposeColor = purposeColor;
        this.activeCards = activeCards;
        this.status = status;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public String getPurpose() { return purpose; }
    public int getPurposeColor() { return purposeColor; }
    public int getActiveCards() { return activeCards; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
