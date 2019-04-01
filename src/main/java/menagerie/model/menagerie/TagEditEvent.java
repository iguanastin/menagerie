package menagerie.model.menagerie;

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
        for (Map.Entry<Item, List<Tag>> entry : added.entrySet()) {
            entry.getValue().forEach(entry.getKey()::removeTag);
        }
        for (Map.Entry<Item, List<Tag>> entry : removed.entrySet()) {
            entry.getValue().forEach(entry.getKey()::addTag);
        }
    }

    public Map<Item, List<Tag>> getAdded() {
        return added;
    }

    public Map<Item, List<Tag>> getRemoved() {
        return removed;
    }

}
