package mx.sgfte.core.users;

import java.math.BigDecimal;

/**
 * One line of the "Gestor de empleados" table (Figma 287:104).
 *
 * The frame puts the department under the name; by preference this shows the
 * email there instead — it identifies the person more usefully in a list, and
 * the department is already filterable from the toolbar.
 */
public class CardholderAdminRow {

    private final long id;
    private final String fullName;
    private final String employeeCode;
    private final String email;
    private final int accountCount;
    private final int cardCount;
    private final BigDecimal totalFunds;
    private final String status;

    public CardholderAdminRow(long id, String fullName, String employeeCode, String email,
                              int accountCount, int cardCount, BigDecimal totalFunds, String status) {
        this.id = id;
        this.fullName = fullName;
        this.employeeCode = employeeCode;
        this.email = email;
        this.accountCount = accountCount;
        this.cardCount = cardCount;
        this.totalFunds = totalFunds;
        this.status = status;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmployeeCode() { return employeeCode; }
    public String getEmail() { return email; }
    public int getAccountCount() { return accountCount; }
    public int getCardCount() { return cardCount; }
    public BigDecimal getTotalFunds() { return totalFunds; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
