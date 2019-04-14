package menagerie.model.menagerie;

import menagerie.gui.thumbnail.Thumbnail;
import menagerie.util.listeners.PokeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Menagerie Item
 */
public abstract class Item implements Comparable<Item> {

    protected final Menagerie menagerie;
    protected final int id;
    private final long dateAdded;
    private final List<Tag> tags = new ArrayList<>();
    private PokeListener tagListener = null;


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
     *
     * @return The unique ID of this item.
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @return The thumbnail of this item. May be null.
     */
    public abstract Thumbnail getThumbnail();

    /**
     *
     * @return The tags this item is tagged with.
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     *
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
        t.incrementFrequency();

        menagerie.getDatabaseManager().tagItemAsync(id, t.getId());

        if (tagListener != null) tagListener.poke();

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
        t.decrementFrequency();

        menagerie.getDatabaseManager().untagItemAsync(id, t.getId());

        if (tagListener != null) tagListener.poke();

        return true;
    }

    /**
     *
     * @param tagListener Listener that is notified when tags are added or removed.
     */
    public void setTagListener(PokeListener tagListener) {
        this.tagListener = tagListener;
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
