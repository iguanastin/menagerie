package menagerie.model.menagerie;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import menagerie.gui.Thumbnail;
import menagerie.model.menagerie.db.DatabaseManager;

/**
 * Menagerie Item
 */
public abstract class Item implements Comparable<Item> {

    private boolean invalidated = false;

    protected final Menagerie menagerie;
    protected final int id;
    private final long dateAdded;
    private final ObservableList<Tag> tags = FXCollections.observableArrayList();


    /**
     * ID uniqueness is not verified by this.
     *
     * @param menagerie Menagerie that owns this item.
     * @param id        Unique ID of this item.
     * @param dateAdded Date this item was added to the Menagerie.
     */
    public Item(Menagerie menagerie, int id, long dateAdded) {
        this.menagerie = menagerie;
        this.id = id;
        this.dateAdded = dateAdded;
    }

    /**
     * @return The date this item was added to the menagerie.
     */
    public long getDateAdded() {
        return dateAdded;
    }

    /**
     * @return The unique ID of this item.
     */
    public int getId() {
        return id;
    }

    /**
     * @return The thumbnail of this item. May be null.
     */
    public abstract Thumbnail getThumbnail();

    public abstract void purgeThumbnail();

    /**
     * @return The tags this item is tagged with.
     */
    public ObservableList<Tag> getTags() {
        return tags;
    }

    /**
     * @param t Tag to find.
     * @return True if this item has the tag.
     */
    public boolean hasTag(Tag t) {
        if (t == null) return false;
        return tags.contains(t);
    }

    /**
     * Tries to add a tag to this item.
     *
     * @param t Tag to add.
     * @return True if this tag was added to this item. False otherwise.
     */
    public boolean addTag(Tag t) {
        if (t == null || hasTag(t)) return false;

        tags.add(t);
        if (!isInvalidated()) {
            t.incrementFrequency();

            if (hasDatabase()) menagerie.getDatabaseManager().tagItemAsync(id, t.getId());
        }

        return true;
    }

    /**
     * Tries to remove a tag from this item.
     *
     * @param t Tag to remove.
     * @return True if the tag was removed.
     */
    public boolean removeTag(Tag t) {
        if (t == null || !hasTag(t)) return false;

        tags.remove(t);
        if (!isInvalidated()) {
            t.decrementFrequency();

            if (hasDatabase()) menagerie.getDatabaseManager().untagItemAsync(id, t.getId());
        }

        return true;
    }

    /**
     * @return True if this item is connected to a Menagerie with a database.
     */
    protected boolean hasDatabase() {
        return menagerie != null && menagerie.getDatabaseManager() != null;
    }

    /**
     * @return The database backing this item's Menagerie.
     */
    protected DatabaseManager getDatabase() {
        if (hasDatabase()) {
            return menagerie.getDatabaseManager();
        } else {
            return null;
        }
    }

    /**
     * Forgets this item from the menagerie. No effect if not in a menagerie, or invalid.
     *
     * @return True if successful.
     */
    protected boolean forget() {
        if (isInvalidated() || menagerie == null || !menagerie.getItems().remove(this)) return false;

        menagerie.itemRemoved(this);
        if (hasDatabase()) getDatabase().removeItemAsync(getId());
        getTags().forEach(Tag::decrementFrequency);
        invalidate();

        return true;
    }

    /**
     * Forgets this item.
     *
     * @return True if successful.
     */
    protected boolean delete() {
        return forget();
    }

    /**
     * Marks this item as invalid. It should not be used in any Menagerie.
     */
    private void invalidate() {
        invalidated = true;
    }

    /**
     * @return True if this item is invalid and should not be used in any Menagerie.
     */
    public boolean isInvalidated() {
        return invalidated;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Item && ((Item) obj).getId() == getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }

    @Override
    public int compareTo(Item o) {
        return getId() - o.getId();
    }

}
