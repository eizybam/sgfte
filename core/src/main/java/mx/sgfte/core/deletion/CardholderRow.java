package mx.sgfte.core.deletion;

/** Small read model for the employees list. */
public class CardholderRow {
    private final long id;
    private final String fullName;
    public CardholderRow(long id, String fullName) { this.id = id; this.fullName = fullName; }
    public long getId() { return id; }
    public String getFullName() { return fullName; }
}
