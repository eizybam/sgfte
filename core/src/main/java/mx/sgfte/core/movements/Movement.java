package mx.sgfte.core.movements;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movement {
    private long id;
    private long accountId;
    private String movementType;
    private BigDecimal amount;
    private Long relatedAccountId;
    private String description;
    private LocalDateTime createdAt;

    public Movement() {
    }

    public Movement(long accountId, String movementType, BigDecimal amount, Long relatedAccountId, String description) {
        this.accountId = accountId;
        this.movementType = movementType;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
        this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }
    
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public Long getRelatedAccountId() { return relatedAccountId; }
    public void setRelatedAccountId(Long relatedAccountId) { this.relatedAccountId = relatedAccountId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
