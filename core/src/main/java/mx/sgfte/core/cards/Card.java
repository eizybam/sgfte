package mx.sgfte.core.cards;

/**
 * A card: an access point to an account. It holds NO money (the money lives in
 * the account). cardType is PHYSICAL or DIGITAL; status is ACTIVE/INACTIVE/BLOCKED.
 */
public class Card {

    private Long id;
    private Long accountId;
    private String cardType;   // PHYSICAL | DIGITAL
    private String maskedPan;  // e.g. "**** **** **** 4821"
    private String status;     // ACTIVE | INACTIVE | BLOCKED

    public Card() {}

    public Card(Long accountId, String cardType, String maskedPan) {
        this.accountId = accountId;
        this.cardType = cardType;
        this.maskedPan = maskedPan;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getMaskedPan() { return maskedPan; }
    public void setMaskedPan(String maskedPan) { this.maskedPan = maskedPan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
