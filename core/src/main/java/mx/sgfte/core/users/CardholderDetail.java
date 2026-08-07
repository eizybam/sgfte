package mx.sgfte.core.users;

import java.math.BigDecimal;

/** Header and profile summary of "Detalle de Tarjetahabiente" (Figma 2074:294). */
public class CardholderDetail {

    private final long id;
    private final String employeeCode;
    private final String fullName;
    private final String email;
    private final String department;
    private final String status;
    private final BigDecimal totalBalance;
    private final int activeAccounts;
    private final int cardCount;

    public CardholderDetail(long id, String employeeCode, String fullName, String email,
                            String department, String status, BigDecimal totalBalance,
                            int activeAccounts, int cardCount) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.status = status;
        this.totalBalance = totalBalance;
        this.activeAccounts = activeAccounts;
        this.cardCount = cardCount;
    }

    public long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getStatus() { return status; }
    public BigDecimal getTotalBalance() { return totalBalance; }
    public int getActiveAccounts() { return activeAccounts; }
    public int getCardCount() { return cardCount; }
    public boolean isActive() { return "ACTIVE".equals(status); }
}
