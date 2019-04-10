package menagerie.model.menagerie;

import menagerie.gui.thumbnail.Thumbnail;
import menagerie.util.PokeListener;

import java.util.ArrayList;
import java.util.List;

public abstract class Item implements Comparable<Item> {

    protected final Menagerie menagerie;
    protected final int id;
    private final long dateAdded;
    private final List<Tag> tags = new ArrayList<>();
    private PokeListener tagListener = null;

    Item(Menagerie menagerie, int id, long dateAdded) {
        this.menagerie = menagerie;
        this.id = id;
        this.dateAdded = dateAdded;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public int getId() {
        return id;
    }

    public abstract Thumbnail getThumbnail();

    public List<Tag> getTags() {
        return tags;
    }

    public boolean hasTag(Tag t) {
        return tags.contains(t);
    }

    public boolean addTag(Tag t) {
        if (hasTag(t)) return false;
        tags.add(t);
        t.incrementFrequency();

        menagerie.getDatabaseUpdater().tagItemAsync(id, t.getId());

        if (tagListener != null) tagListener.poke();

        return true;
    }

    public boolean removeTag(Tag t) {
        if (!hasTag(t)) return false;
        tags.remove(t);
        t.decrementFrequency();

        menagerie.getDatabaseUpdater().untagItemAsync(id, t.getId());

        if (tagListener != null) tagListener.poke();

        return true;
    }

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
