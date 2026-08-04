package mx.sgfte.core.categories;

/**
 * A purpose in the global catalogue: what an account's money is for.
 *
 * Carries its own colour (1..7) since V5. It used to be worked out from the
 * category's position in the catalogue, which meant inserting one in the middle
 * repainted the ones after it.
 */
public class Category {
    private Long id;
    private String name;
    private String description;
    private int colorIndex = 1;
    private String status = "ACTIVE";

    public Category() {}

    public Category(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Category(Long id, String name, String description, int colorIndex, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.colorIndex = colorIndex;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getColorIndex() { return colorIndex; }
    public void setColorIndex(int colorIndex) { this.colorIndex = colorIndex; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "ACTIVE".equals(status); }
}
