package mx.sgfte.core.accounts;

import java.math.BigDecimal;

/**
 * One line of the "Gestión de Cuentas" table (Figma 225:41): the account code,
 * who it belongs to, what it is for, how many live cards hang off it and
 * whether it is still active.
 *
 * También alimenta la lista de cuentas del detalle de tarjetahabiente, que sí
 * muestra el saldo — de ahí que lo lleve aunque la tabla de Gestión no lo pinte.
 *
 * El correo del titular viaja por el mismo motivo: sólo lo pinta el selector de
 * cuenta destino, donde dos "Raúl Torres" se distinguen por él.
 */
public class AccountRow {

    private final long id;
    private final String accountNumber;
    private final String holderName;
    private final String holderEmail;
    private final String purpose;
    private final int purposeColor;   // 1..4, elige el color del badge
    private final int activeCards;
    private final BigDecimal balance;
    private final String status;      // ACTIVE | INACTIVE

    public AccountRow(long id, String accountNumber, String holderName, String holderEmail,
                      String purpose, int purposeColor, int activeCards,
                      BigDecimal balance, String status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.holderEmail = holderEmail;
        this.purpose = purpose;
        this.purposeColor = purposeColor;
        this.activeCards = activeCards;
        this.balance = balance;
        this.status = status;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public String getHolderEmail() { return holderEmail; }
    public String getPurpose() { return purpose; }
    public int getPurposeColor() { return purposeColor; }
    public int getActiveCards() { return activeCards; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
