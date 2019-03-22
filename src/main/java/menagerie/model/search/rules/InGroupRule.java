package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class InGroupRule extends SearchRule {

    private final int id;


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
