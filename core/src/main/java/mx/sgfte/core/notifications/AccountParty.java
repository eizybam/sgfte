package mx.sgfte.core.notifications;

import java.math.BigDecimal;

/**
 * Who is on one side of a movement: the account and the person who owns it.
 *
 * The notifications module reads this itself instead of asking the accounts or
 * users modules for it. The point is that nothing else has to know notifications
 * exist — if this needed a method added to AccountDao, every change here would
 * ripple outwards, which is the opposite of what was asked for.
 */
public record AccountParty(
        long accountId,
        long cardholderId,
        String accountNumber,
        String purpose,
        String holderName,
        String holderEmail,
        BigDecimal balance) {

    /** "Gasolina · GAS-48HSY", para nombrar la cuenta en el correo. */
    public String label() {
        return purpose + " · " + accountNumber;
    }
}
