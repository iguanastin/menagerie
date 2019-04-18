package menagerie.model.menagerie;

import menagerie.gui.thumbnail.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Menagerie group item containing some media elements.
 */
public class GroupItem extends Item {

    private final List<MediaItem> elements = new ArrayList<>();
    private String title;


    /**
     * ID uniqueness is not verified by this.
     *
     * @param menagerie Menagerie that owns this item.
     * @param id        Unique ID of this item.
     * @param dateAdded Date this item was added to the Menagerie.
     * @param title     Title of this group.
     */
    public GroupItem(Menagerie menagerie, int id, long dateAdded, String title) {
        super(menagerie, id, dateAdded);
        this.title = title;
    }

    /**
     * Adds an item to this group. If the item is currently in another group, it is removed from that group first.
     *
     * @param item Item to add.
     * @return True if successful. False if item is already in this group.
     */
    public boolean addItem(MediaItem item) {
        if (elements.contains(item)) return false;

        if (item.inGroup()) item.getGroup().removeItem(item);

        elements.add(item);
        item.setGroup(this);

        updateIndices();
        if (menagerie != null) menagerie.refreshInSearches(Collections.singletonList(item));
        return true;
    }

    /**
     * Attempts to remove an item from this group. Does nothing if the item is not in this group.
     *
     * @param item Item to remove.
     */
    public boolean removeItem(MediaItem item) {
        if (elements.remove(item)) {
            item.setGroup(null);
            updateIndices();
            if (menagerie != null) menagerie.refreshInSearches(Collections.singletonList(item));
            return true;
        }

        return false;
    }

    /**
     * Removes all items from this group.
     */
    public void removeAll() {
        Iterator<MediaItem> iter = elements.iterator();
        while (iter.hasNext()) {
            MediaItem item = iter.next();
            iter.remove();
            item.setGroup(null);
            if (menagerie != null) menagerie.refreshInSearches(Collections.singletonList(item));
        }
    }

    /**
     * Moves elements to a specific place in the elements list.
     *
     * @param list         List of elements to move. Must all be items in this group.
     * @param anchor       Anchor element to move relative to. Must not be in list of items to move.
     * @param beforeAnchor Moves the elements before the anchor instead of after it.
     * @return True if successfully moved.
     */
    public boolean moveElements(List<MediaItem> list, MediaItem anchor, boolean beforeAnchor) {
        if (list.contains(anchor) || !elements.contains(anchor) || !elements.containsAll(list)) return false;

        elements.removeAll(list);

        int anchorIndex = elements.indexOf(anchor);
        if (!beforeAnchor) anchorIndex++;
        for (int i = 0; i < list.size(); i++) {
            elements.add(anchorIndex + i, list.get(i));
        }

        updateIndices();
        return true;
    }

    /**
     * Checks all elements and updates their page index if not synced.
     */
    private void updateIndices() {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getPageIndex() != i) {
                elements.get(i).setPageIndex(i);
            }
        }
    }

    /**
     * @return The thumbnail of the first element in this group. Null if group is empty.
     */
    @Override
    public Thumbnail getThumbnail() {
        if (elements.isEmpty()) {
            return null;
        } else {
            return elements.get(0).getThumbnail();
        }
    }

    /**
     * @return The title of this group.
     */
    public synchronized String getTitle() {
        return title;
    }

    /**
     * @return The elements of this group.
     */
    public List<MediaItem> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return getId() + " (" + elements.size() + "): " + title;
    }

}
