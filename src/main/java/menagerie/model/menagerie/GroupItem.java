package menagerie.model.menagerie;

import menagerie.gui.thumbnail.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GroupItem extends Item {

    private final List<MediaItem> elements = new ArrayList<>();
    private String title;


    GroupItem(Menagerie menagerie, int id, long dateAdded, String title) {
        super(menagerie, id, dateAdded);
        this.title = title;
    }

    public void addItem(MediaItem item) {
        elements.add(item);
        item.setGroup(this);
        menagerie.checkItemsStillValidInSearches(Collections.singletonList(item));

        //TODO: Update database
    }

    public void removeItem(MediaItem item) {
        elements.remove(item);
        item.setGroup(null);
        menagerie.checkItemsStillValidInSearches(Collections.singletonList(item));

        //TODO: Update database
    }

    public void removeAll() {
        Iterator<MediaItem> iter = elements.iterator();
        while (iter.hasNext()) {
            removeItem(iter.next());
        }
    }

    @Override
    public Thumbnail getThumbnail() {
        if (elements.isEmpty()) {
            return null;
        } else {
            return elements.get(0).getThumbnail();
        }
    }

    public synchronized void setTitle(String title) {
        this.title = title;
        menagerie.checkItemsStillValidInSearches(Collections.singletonList(this));

        //TODO: Update database
    }

    public synchronized String getTitle() {
        return title;
    }

    public List<MediaItem> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return getId() + " (" + elements.size() + "): " + title;
    }

}
