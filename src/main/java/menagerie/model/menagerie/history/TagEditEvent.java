package menagerie.model.menagerie.history;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Tag;

import java.util.List;
import java.util.Map;

public class TagEditEvent {

    private final Map<ImageInfo, List<Tag>> added;
    private final Map<ImageInfo, List<Tag>> removed;


    public TagEditEvent(Map<ImageInfo, List<Tag>> added, Map<ImageInfo, List<Tag>> removed) {
        this.added = added;
        this.removed = removed;

    }

    public void reverseAction() {
        for (ImageInfo item : added.keySet()) {
            added.get(item).forEach(item::removeTag);
        }
        for (ImageInfo item : removed.keySet()) {
            removed.get(item).forEach(item::addTag);
        }
    }

    public int getNumChanges() {
        int n = 0;

        for (ImageInfo item : added.keySet()) {
            n += added.get(item).size();
        }
        for (ImageInfo item : removed.keySet()) {
            n += removed.get(item).size();
        }

        return n;
    }

    public Map<ImageInfo, List<Tag>> getAdded() {
        return added;
    }

    public Map<ImageInfo, List<Tag>> getRemoved() {
        return removed;
    }

}
