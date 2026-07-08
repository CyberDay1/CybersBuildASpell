package buildaspell.item;

/**
 * The three wand tiers. Ordinal order is the power order (Worn &lt; Carved &lt; Runic) and is used
 * to pick the strongest wand a player is holding. The serialized name is the config sub-section
 * and item id stem (worn_wand, carved_wand, runic_wand).
 */
public enum WandTier {
    WORN("worn"),
    CARVED("carved"),
    RUNIC("runic");

    private final String id;

    WandTier(String id) {
        this.id = id;
    }

    public String getSerializedName() {
        return id;
    }
}
