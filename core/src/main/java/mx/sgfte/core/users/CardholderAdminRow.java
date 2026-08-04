package mx.sgfte.core.users;

import java.math.BigDecimal;

/**
 * One line of the "Gestor de empleados" table (Figma 287:104).
 *
 * employeeCode and department landed with V2; before that the view had to fall
 * back to the primary key and the email, which is no longer the case.
 */
public class CardholderAdminRow {

    private final long id;
    private final String fullName;
    private final String employeeCode;
    private final String department;
    private final int accountCount;
    private final int cardCount;
    private final BigDecimal totalFunds;
    private final String status;

    public CardholderAdminRow(long id, String fullName, String employeeCode, String department,
                              int accountCount, int cardCount, BigDecimal totalFunds, String status) {
        this.id = id;
        this.fullName = fullName;
        this.employeeCode = employeeCode;
        this.department = department;
        this.accountCount = accountCount;
        this.cardCount = cardCount;
        this.totalFunds = totalFunds;
        this.status = status;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmployeeCode() { return employeeCode; }
    public String getDepartment() { return department; }
    public int getAccountCount() { return accountCount; }
    public int getCardCount() { return cardCount; }
    public BigDecimal getTotalFunds() { return totalFunds; }
    public String getStatus() { return status; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
