package mx.sgfte.core.accounts;

import java.math.BigDecimal;

/**
 * One line of "Movimientos recientes".
 *
 * The date arrives already formatted because JSTL's fmt tags cannot handle
 * java.time, and the alternative — converting in the page — would put
 * formatting logic in the view.
 *
 * There is no "pending" state to model: a movement is only written inside the
 * transaction that already moved the money, and the ledger is immutable, so
 * every row that exists is settled. The frame shows a PENDIENTE badge, but
 * nothing in this system can produce one.
 */
public class AccountMovementRow {

    private final String dayLabel;   // "12 Jun"
    private final String concept;
    private final BigDecimal amount;
    private final boolean inflow;    // suma al saldo de la cuenta

    public AccountMovementRow(String dayLabel, String concept, BigDecimal amount, boolean inflow) {
        this.dayLabel = dayLabel;
        this.concept = concept;
        this.amount = amount;
        this.inflow = inflow;
    }

    public String getDayLabel() { return dayLabel; }
    public String getConcept() { return concept; }
    public BigDecimal getAmount() { return amount; }
    public boolean isInflow() { return inflow; }
}
