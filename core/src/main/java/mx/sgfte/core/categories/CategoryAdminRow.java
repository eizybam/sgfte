package mx.sgfte.core.categories;

import java.math.BigDecimal;

/** One row of the "Categorías - Admin" table (Figma 2036:6). */
public record CategoryAdminRow(
        long id,
        String name,
        String description,
        int colorIndex,
        String status,
        int accounts,
        BigDecimal dispersed) {

    public boolean isActive() { return "ACTIVE".equals(status); }

    /** JSTL reaches records through getters, not through the component names. */
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getColorIndex() { return colorIndex; }
    public String getStatus() { return status; }
    public int getAccounts() { return accounts; }
    public BigDecimal getDispersed() { return dispersed; }
    public boolean getActive() { return isActive(); }
}
