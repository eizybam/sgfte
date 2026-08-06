package mx.sgfte.core.portal;

/**
 * A colleague's account that is a legal P2P destination for the chosen source
 * account: same purpose (category), active, and belonging to someone else.
 *
 * Note there is no balance here. The employee has no business knowing how much
 * money a colleague holds (RNF-05), so the read model simply doesn't carry it.
 */
public class PeerOption {

    private final long accountId;
    private final String label;   // e.g. "Ana López — GAS-48HSY"

    public PeerOption(long accountId, String label) {
        this.accountId = accountId;
        this.label = label;
    }

    public long getAccountId() { return accountId; }
    public String getLabel() { return label; }
}
