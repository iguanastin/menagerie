package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

/**
 * Rule that checks if an item is in a group.
 */
public class InGroupRule extends SearchRule {

    private final int id;


    /**
     * @param id       ID of group.
     * @param inverted Negate this rule.
     */
    public InGroupRule(int id, boolean inverted) {
        super(inverted);
        this.id = id;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = item instanceof MediaItem && ((MediaItem) item).getGroup() != null && ((MediaItem) item).getGroup().getId() == id;
        if (isInverted()) result = !result;
        return result;
    }

    @Override
    public String toString() {
        String result = "Group Rule: " + id;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
