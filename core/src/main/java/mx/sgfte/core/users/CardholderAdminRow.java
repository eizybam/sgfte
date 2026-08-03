package mx.sgfte.core.users;

import java.math.BigDecimal;

/**
 * One line of the "Gestor de empleados" table (Figma 287:104).
 *
 * The frame also shows a department under the name and a code like "AM84920" in
 * the ID column. Neither exists: `cardholder` holds only names, email, phone and
 * status. The email stands in for the second line — it is the one other
 * identifying detail the table can show — and the ID column carries the real
 * primary key. Both are noted in the view.
 */
public class CardholderAdminRow {

    private final long id;
    private final String fullName;
    private final String email;
    private final int accountCount;
    private final int cardCount;
    private final BigDecimal totalFunds;
    private final String status;

    public CardholderAdminRow(long id, String fullName, String email, int accountCount,
                              int cardCount, BigDecimal totalFunds, String status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.accountCount = accountCount;
        this.cardCount = cardCount;
        this.totalFunds = totalFunds;
        this.status = status;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public int getAccountCount() { return accountCount; }
    public int getCardCount() { return cardCount; }
    public BigDecimal getTotalFunds() { return totalFunds; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
