package mx.sgfte.core.portal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Read model for one of the employee's own cards, across ANY of their
 * accounts — unlike {@code Card}, which knows its accountId but not what
 * that account actually is.
 *
 * "Mis tarjetas" spans every account the cardholder owns, so each row has to
 * carry enough about its account (number, purpose, purpose colour) to tell
 * cards apart at a glance without a second lookup per row.
 */
public class PortalCard {

    private static final DateTimeFormatter MM_YY = DateTimeFormatter.ofPattern("MM/yy");

    private final long id;
    private final long accountId;
    private final String accountNumber;
    private final String purpose;
    private final int colorIndex;
    private final String cardType;    // PHYSICAL | DIGITAL
    private final String maskedPan;
    private final LocalDate issuedAt;
    private final LocalDate expiresAt;

    public PortalCard(long id, long accountId, String accountNumber, String purpose, int colorIndex,
                      String cardType, String maskedPan, LocalDate issuedAt, LocalDate expiresAt) {
        this.id = id;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.purpose = purpose;
        this.colorIndex = colorIndex;
        this.cardType = cardType;
        this.maskedPan = maskedPan;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public long getId() { return id; }
    public long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getPurpose() { return purpose; }
    public int getColorIndex() { return colorIndex; }
    public String getMaskedPan() { return maskedPan; }

    public boolean isPhysical() { return "PHYSICAL".equals(cardType); }
    public String getTypeLabel() { return isPhysical() ? "Física" : "Digital"; }

    public String getIssuedLabel()  { return issuedAt  == null ? "—" : MM_YY.format(issuedAt); }
    public String getExpiresLabel() { return expiresAt == null ? "—" : MM_YY.format(expiresAt); }

    /** Sprite id for the account's purpose icon, same mapping the account cards use. */
    public String getIcon() { return PortalIcons.forPurpose(purpose); }
}
