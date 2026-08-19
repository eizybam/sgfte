package mx.sgfte.core.movements;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the immutable ledger (account_movement).
 * movementType is one of DEPOSIT | WITHDRAWAL | TRANSFER_IN | TRANSFER_OUT | REINTEGRATION.
 * relatedAccountId is only used for P2P transfers (the counterparty).
 */
public class Movement {

    private Long id;
    private Long accountId;
    private String movementType;
    private BigDecimal amount;
    private Long relatedAccountId;   // nullable
    private String description;
    /**
     * Con qué tarjeta se hizo el consumo (V13). Sólo lo lleva un WITHDRAWAL:
     * un depósito o una transferencia no pasan por ningún plástico, y un CHECK
     * de la base lo confirma.
     */
    private Long cardId;             // nullable
    private LocalDateTime createdAt;

    public Movement() {}

    public Movement(Long accountId, String movementType, BigDecimal amount,
                    Long relatedAccountId, String description) {
        this(accountId, movementType, amount, relatedAccountId, description, null);
    }

    public Movement(Long accountId, String movementType, BigDecimal amount,
                    Long relatedAccountId, String description, Long cardId) {
        this.accountId = accountId;
        this.movementType = movementType;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
        this.description = description;
        this.cardId = cardId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getRelatedAccountId() { return relatedAccountId; }
    public void setRelatedAccountId(Long relatedAccountId) { this.relatedAccountId = relatedAccountId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
