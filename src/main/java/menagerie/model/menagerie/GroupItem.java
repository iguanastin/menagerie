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
     */
    public void addItem(MediaItem item) {
        if (item.inGroup()) item.getGroup().removeItem(item);
        elements.add(item);
        item.setGroup(this);
        menagerie.checkItemsStillValidInSearches(Collections.singletonList(item));

        //TODO: Update database
    }

    /**
     * Attempts to remove an item from this group. Does nothing if the item is not in this group.
     *
     * @param item Item to remove.
     */
    public void removeItem(MediaItem item) {
        if (elements.contains(item)) {
            elements.remove(item);
            item.setGroup(null);
            menagerie.checkItemsStillValidInSearches(Collections.singletonList(item));

            // TODO: Update database
        }
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
            menagerie.checkItemsStillValidInSearches(Collections.singletonList(item));

            // TODO: Update database
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
     * Sets the title of this group.
     *
     * @param title New title
     */
    public synchronized void setTitle(String title) {
        if (this.title.equals(title)) return;

        this.title = title;
        menagerie.checkItemsStillValidInSearches(Collections.singletonList(this));

        //TODO: Update database
    }

    /**
     *
     * @return The title of this group.
     */
    public synchronized String getTitle() {
        return title;
    }

    /**
     *
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
