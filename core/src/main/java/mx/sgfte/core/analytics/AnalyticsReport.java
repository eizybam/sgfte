package mx.sgfte.core.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Todo lo que la pantalla de Analíticas muestra, en un solo objeto: los KPI con
 * su variación, las dos gráficas, los dos repartos por propósito y el top de
 * tarjetahabientes — todo medido sobre la MISMA ventana.
 *
 * Es una clase normal con getters y setters, no un record, y no por descuido:
 * este objeto puede acabar en una JSP, y el EL 5.0 de Tomcat 10.1 resuelve
 * ${x.foo} llamando a getFoo() — los accesores de un record (foo()) le son
 * invisibles. Un record aquí reventaría a media renderización y, con el buffer
 * de 8 KB ya vaciado, la respuesta saldría truncada en vez de dar un 500. Es el
 * bug que costó una tarde en la Concentradora; ver esa guía.
 *
 * Los delta pueden ser null: significa "no hay contra qué comparar", que no es
 * lo mismo que 0%. La pantalla los pinta como raya y el CSV los deja vacíos.
 */
public class AnalyticsReport {

    private AnalyticsPeriod period;
    private LocalDateTime from;
    private LocalDateTime to;

    private BigDecimal concentratorBalance;
    private BigDecimal concentratorDelta;
    private BigDecimal dispersed;
    private BigDecimal dispersedDelta;

    private int activeCardholders;
    private int newCardholders;
    private int activeCards;
    private int physicalCards;
    private int digitalCards;

    private List<ChartBar> dispersionBars;
    private List<PurposeShare> purposes;
    private List<ChartBar> weekdayBars;
    private int weekTotal;
    private List<TopSpender> topSpenders;

    private BigDecimal transfersTotal;
    private int transfersCount;
    private List<PurposeShare> transferPurposes;

    public AnalyticsPeriod getPeriod() { return period; }
    public void setPeriod(AnalyticsPeriod period) { this.period = period; }

    public LocalDateTime getFrom() { return from; }
    public void setFrom(LocalDateTime from) { this.from = from; }

    public LocalDateTime getTo() { return to; }
    public void setTo(LocalDateTime to) { this.to = to; }

    public BigDecimal getConcentratorBalance() { return concentratorBalance; }
    public void setConcentratorBalance(BigDecimal v) { this.concentratorBalance = v; }

    public BigDecimal getConcentratorDelta() { return concentratorDelta; }
    public void setConcentratorDelta(BigDecimal v) { this.concentratorDelta = v; }

    public BigDecimal getDispersed() { return dispersed; }
    public void setDispersed(BigDecimal v) { this.dispersed = v; }

    public BigDecimal getDispersedDelta() { return dispersedDelta; }
    public void setDispersedDelta(BigDecimal v) { this.dispersedDelta = v; }

    public int getActiveCardholders() { return activeCardholders; }
    public void setActiveCardholders(int v) { this.activeCardholders = v; }

    public int getNewCardholders() { return newCardholders; }
    public void setNewCardholders(int v) { this.newCardholders = v; }

    public int getActiveCards() { return activeCards; }
    public void setActiveCards(int v) { this.activeCards = v; }

    public int getPhysicalCards() { return physicalCards; }
    public void setPhysicalCards(int v) { this.physicalCards = v; }

    public int getDigitalCards() { return digitalCards; }
    public void setDigitalCards(int v) { this.digitalCards = v; }

    public List<ChartBar> getDispersionBars() { return dispersionBars; }
    public void setDispersionBars(List<ChartBar> v) { this.dispersionBars = v; }

    public List<PurposeShare> getPurposes() { return purposes; }
    public void setPurposes(List<PurposeShare> v) { this.purposes = v; }

    public List<ChartBar> getWeekdayBars() { return weekdayBars; }
    public void setWeekdayBars(List<ChartBar> v) { this.weekdayBars = v; }

    public int getWeekTotal() { return weekTotal; }
    public void setWeekTotal(int v) { this.weekTotal = v; }

    public List<TopSpender> getTopSpenders() { return topSpenders; }
    public void setTopSpenders(List<TopSpender> v) { this.topSpenders = v; }

    public BigDecimal getTransfersTotal() { return transfersTotal; }
    public void setTransfersTotal(BigDecimal v) { this.transfersTotal = v; }

    public int getTransfersCount() { return transfersCount; }
    public void setTransfersCount(int v) { this.transfersCount = v; }

    public List<PurposeShare> getTransferPurposes() { return transferPurposes; }
    public void setTransferPurposes(List<PurposeShare> v) { this.transferPurposes = v; }
}
