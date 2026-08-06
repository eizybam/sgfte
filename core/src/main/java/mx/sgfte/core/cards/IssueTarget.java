package mx.sgfte.core.cards;

import java.math.BigDecimal;

/**
 * One account a card can be issued against, with everything the "Expedir
 * Tarjeta" screen shows about it.
 *
 * The existing AccountOption only carries an id, a display label and a balance,
 * which is enough for the dispersion dropdown but not for this screen: the
 * Figma frame (2038:129) also shows the cardholder, the account number and the
 * purpose in the preview card and the summary panel. Rather than firing a query
 * per selection, the screen loads these once and picks between them in the
 * browser.
 */
public class IssueTarget {

    private final long accountId;
    private final String accountNumber;   // p. ej. "G-26D3HD"
    private final String purpose;         // categoría de la cuenta
    private final BigDecimal balance;
    private final long cardholderId;
    private final String cardholderName;  // "Nombre Apellido"

    public IssueTarget(long accountId, String accountNumber, String purpose,
                       BigDecimal balance, long cardholderId, String cardholderName) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.purpose = purpose;
        this.balance = balance;
        this.cardholderId = cardholderId;
        this.cardholderName = cardholderName;
    }

    public long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getPurpose() { return purpose; }
    public BigDecimal getBalance() { return balance; }
    public long getCardholderId() { return cardholderId; }
    public String getCardholderName() { return cardholderName; }
}
