package menagerie.model.menagerie;

import menagerie.gui.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
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

        if (item.isInGroup()) item.getGroup().removeItem(item);

        elements.add(item);
        item.setGroup(this);

        if (!isInvalidated()) updateIndices();
        if (menagerie != null) menagerie.refreshInSearches(item);
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
            if (!isInvalidated()) updateIndices();
            if (menagerie != null) menagerie.refreshInSearches(item);
            return true;
        }

        return false;
    }

    /**
     * Removes all items from this group.
     */
    public void removeAll() {
        List<Item> temp = new ArrayList<>(elements);
        elements.forEach(mediaItem -> mediaItem.setGroup(null));
        elements.clear();
        if (menagerie != null) menagerie.refreshInSearches(temp);
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

        if (!isInvalidated()) updateIndices();
        return true;
    }

    /**
     * Reverses the order of elements in this group.
     */
    public void reverseElements() {
        Collections.reverse(elements);

        if (!isInvalidated()) updateIndices();
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

    @Override
    public void purgeThumbnail() {
        if (!elements.isEmpty()) {
            elements.get(0).purgeThumbnail();
        }
    }

    /**
     * @return The title of this group.
     */
    public synchronized String getTitle() {
        return title;
    }

    /**
     * Sets the title of this group.
     *
     * @param str New title.
     */
    public void setTitle(String str) {
        title = str;

        if (!isInvalidated()) {
            if (hasDatabase()) menagerie.getDatabaseManager().setGroupTitleAsync(getId(), title);
            if (menagerie != null) menagerie.refreshInSearches(this);
        }
    }

    /**
     * @return The elements of this group.
     */
    public List<MediaItem> getElements() {
        return elements;
    }

    /**
     * Forgets this group and all its elements from the Menagerie.
     *
     * @return True if successfully forgotten.
     */
    @Override
    protected boolean forget() {
        if (!super.forget()) return false;

        for (MediaItem item : elements) {
            item.forget();
        }

        return true;
    }

    /**
     * Forgets and deletes this group and all its elements. Element files are deleted.
     *
     * @return True if successfully deleted.
     */
    @Override
    protected boolean delete() {
        if (!super.forget()) return false;

        for (MediaItem item : elements) {
            item.delete();
        }

        return true;
    }

    @Override
    public String toString() {
        return getId() + " (" + elements.size() + "): " + title;
    }

}
