package menagerie.model.menagerie.history;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Tag;

import java.util.List;
import java.util.Map;

public class TagEditEvent {

    private final Map<Item, List<Tag>> added;
    private final Map<Item, List<Tag>> removed;


    public TagEditEvent(Map<Item, List<Tag>> added, Map<Item, List<Tag>> removed) {
        this.added = added;
        this.removed = removed;

    }

    public void reverseAction() {
        for (Item item : added.keySet()) {
            added.get(item).forEach(item::removeTag);
        }
        for (Item item : removed.keySet()) {
            removed.get(item).forEach(item::addTag);
        }
    }

    public int getNumChanges() {
        int n = 0;

        for (Item item : added.keySet()) {
            n += added.get(item).size();
        }
        for (Item item : removed.keySet()) {
            n += removed.get(item).size();
        }

        return n;
    }

    public Map<Item, List<Tag>> getAdded() {
        return added;
    }

    public Map<Item, List<Tag>> getRemoved() {
        return removed;
    }

}
