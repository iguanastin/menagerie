package menagerie.model.menagerie;

import java.util.List;
import java.util.Map;

/**
 * Records an event where a set of items had tags added or removed from them.
 */
public class TagEditEvent {

    private final Map<Item, List<Tag>> added;
    private final Map<Item, List<Tag>> removed;


    /**
     * @param added   Map of Items and the Tags that were added to each of them.
     * @param removed Map of Items and the Tags that were removed from each of them.
     */
    public TagEditEvent(Map<Item, List<Tag>> added, Map<Item, List<Tag>> removed) {
        this.added = added;
        this.removed = removed;

    }

    /**
     * Undo this event by adding back tags that were removed and removing tags that were added. Item's tags should be as they were before the event that created this record occurred.
     */
    public void revertAction() {
        for (Map.Entry<Item, List<Tag>> entry : added.entrySet()) {
            entry.getValue().forEach(entry.getKey()::removeTag);
        }
        for (Map.Entry<Item, List<Tag>> entry : removed.entrySet()) {
            entry.getValue().forEach(entry.getKey()::addTag);
        }
    }

    /**
     * @return The Map of Items and Tags that were added.
     */
    public Map<Item, List<Tag>> getAdded() {
        return added;
    }

    /**
     * @return The map of Items and Tags that were removed.
     */
    public Map<Item, List<Tag>> getRemoved() {
        return removed;
    }

}
