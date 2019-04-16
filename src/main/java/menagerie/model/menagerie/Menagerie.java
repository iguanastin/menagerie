package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
import menagerie.gui.Main;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.search.Search;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Menagerie system. Contains items, manages database.
 */
public class Menagerie {

    // ------------------------------ Variables -----------------------------------

    private final List<Item> items = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();

    private int nextItemID;
    private int nextTagID;

    private final DatabaseManager databaseManager;

    private final List<Search> activeSearches = new ArrayList<>();


    /**
     * Constructs a Menagerie. Starts a database updater thread, loads tags and media info from database, prunes database.
     *
     * @param databaseManager Database manager to back this menagerie.
     * @throws SQLException If any errors occur in database loading/prep.
     */
    public Menagerie(DatabaseManager databaseManager) throws SQLException {
        this.databaseManager = databaseManager;

        // Load data from database
        databaseManager.loadIntoMenagerie(this);

        clearUnusedTags();

        nextItemID = databaseManager.getHighestItemID() + 1;
        nextTagID = databaseManager.getHighestTagID() + 1;
    }

    /**
     * Removes all unused tags from the database.
     *
     * @throws SQLException If any error occurs in database.
     */
    private void clearUnusedTags() throws SQLException {
        Set<Integer> usedTags = new HashSet<>();
        for (Item img : items) {
            for (Tag t : img.getTags()) {
                usedTags.add(t.getId());
            }
        }
        for (Tag t : new ArrayList<>(tags)) {
            if (!usedTags.contains(t.getId())) {
                Main.log.info("Deleting unused tag: " + t);
                tags.remove(t);
                getDatabaseManager().deleteTag(t.getId());
            }
        }
    }

    /**
     * Attempts to import a file into this Menagerie. Will fail if file is already present.
     *
     * @param file File to import.
     * @return The MediaItem for the imported file, or null if import failed.
     */
    public MediaItem importFile(File file, boolean tagTagme, boolean tagVideo, boolean tagImage) {
        if (isFilePresent(file)) return null;

        MediaItem media = new MediaItem(this, nextItemID, System.currentTimeMillis(), 0, null, file, null, null);

        // Add media and commit to database
        items.add(media);
        nextItemID++;
        try {
            getDatabaseManager().createMedia(media);
        } catch (SQLException e) {
            Main.log.log(Level.SEVERE, "Failed to create media in database: " + media, e);
            return null;
        }

        // Add tags
        if (tagTagme) {
            Tag tagme = getTagByName("tagme");
            if (tagme == null) tagme = createTag("tagme");
            media.addTag(tagme);
        }
        if (tagImage && media.isImage()) {
            Tag image = getTagByName("image");
            if (image == null) image = createTag("image");
            media.addTag(image);
        }
        if (tagVideo && media.isVideo()) {
            Tag video = getTagByName("video");
            if (video == null) video = createTag("video");
            media.addTag(video);
        }

        //Update active searches
        activeSearches.forEach(search -> search.addIfValid(Collections.singletonList(media)));

        return media;
    }

    /**
     * Creates a group and adds elements to it.
     *
     * @param elements Elements to be added. If an element is a GroupItem, that group's contents will be removed and added to the new group.
     * @param title    Title of the new group.
     * @return The newly created group. Null if title is null or empty, and null if element list is null or empty.
     */
    public GroupItem createGroup(List<Item> elements, String title, boolean tagTagme) {
        if (title == null || title.isEmpty() || elements == null || elements.isEmpty()) return null;

        GroupItem group = new GroupItem(this, nextItemID, System.currentTimeMillis(), title);

        nextItemID++;
        items.add(group);
        try {
            getDatabaseManager().createGroup(group);
        } catch (SQLException e) {
            Main.log.log(Level.SEVERE, "Error storing group in database: " + group, e);
            return null;
        }

        for (Item item : elements) {
            if (item instanceof MediaItem && ((MediaItem) item).getGroup() == null) {
                group.addItem((MediaItem) item);
            } else if (item instanceof GroupItem) {
                List<MediaItem> e = new ArrayList<>(((GroupItem) item).getElements());
                removeItems(Collections.singletonList(item), false);
                e.forEach(group::addItem);
            } else {
                return null;
            }
        }

        if (tagTagme) {
            Tag tagme = getTagByName("tagme");
            if (tagme == null) tagme = createTag("tagme");
            group.addTag(tagme);
        }

        // Update searches
        checkItemsStillValidInSearches(Collections.singletonList(group));

        return group;
    }

    /**
     * Creates a new tag in this Menagerie.
     *
     * @param name Name of new tag. Must be unique (case insensitive).
     * @return The newly created tag, or null if name is not unique or name is invalid.
     */
    public Tag createTag(String name) {
        Tag t;
        try {
            t = new Tag(this, nextTagID, name, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
        nextTagID++;

        tags.add(t);

        getDatabaseManager().createTagAsync(t.getId(), t.getName());

        return t;
    }

    /**
     * Removes items from this Menagerie.
     *
     * @param items       Items to be removed.
     * @param deleteFiles Delete the files after removing them. Files will be moved to recycle bin if possible.
     */
    public void removeItems(List<Item> items, boolean deleteFiles) {
        List<Item> removed = new ArrayList<>();
        List<MediaItem> toDelete = new ArrayList<>();

        for (Item item : items) {
            if (getItems().remove(item)) {
                item.getTags().forEach(Tag::decrementFrequency);

                removed.add(item);
                if (deleteFiles && item instanceof MediaItem) toDelete.add((MediaItem) item);
                if (item instanceof GroupItem) {
                    activeSearches.forEach(search -> search.recheckWithSearch(new ArrayList<Item>(((GroupItem) item).getElements())));
                    ((GroupItem) item).removeAll();
                }

                getDatabaseManager().removeItemAsync(item.getId());
            }
        }

        if (deleteFiles) {
            FileUtils fu = FileUtils.getInstance();
            if (fu.hasTrash()) {
                try {
                    File[] fa = new File[toDelete.size()];
                    for (int i = 0; i < fa.length; i++) fa[i] = toDelete.get(i).getFile();
                    fu.moveToTrash(fa);
                } catch (IOException e) {
                    Main.log.log(Level.SEVERE, String.format("Unable to send %d files to recycle bin", toDelete.size()), e);
                }
            } else {
                toDelete.forEach(item -> {
                    if (item.getFile().delete()) {
                        Main.log.severe(String.format("Unable to delete file: %s", item.getFile().toString()));
                    }
                });
                return;
            }
        }

        activeSearches.forEach(search -> search.remove(removed));
    }

    /**
     * @return All tags in the Menagerie environment.
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Attempts to find a tag given a tag id.
     *
     * @param id ID of tag to find.
     * @return Tag with given ID, or null if none exist.
     */
    public Tag getTagByID(int id) {
        for (Tag t : tags) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    /**
     * Attempts to find a tag given a tag name.
     * <p>
     * Case insensitive.
     *
     * @param name Name of tag to find.
     * @return Tag with given name, or null if none exist.
     */
    public Tag getTagByName(String name) {
        name = name.replace(' ', '_');
        for (Tag t : tags) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    /**
     * Check with all active searches to see if items are still valid or need to be removed. This method should be called after an item is modified.
     *
     * @param items Items to check.
     */
    public void checkItemsStillValidInSearches(List<Item> items) {
        activeSearches.forEach(search -> search.recheckWithSearch(items));
    }

    /**
     * @return All items in this Menagerie.
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * @return The database updater thread backing this Menagerie.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * @param file File to search for.
     * @return True if this file has already been imported into this Menagerie.
     */
    private boolean isFilePresent(File file) {
        for (Item item : items) {
            if (item instanceof MediaItem && ((MediaItem) item).getFile().equals(file)) return true;
        }
        return false;
    }

    /**
     * Unregisters a search from this Menagerie.
     *
     * @param search Search to unregister.
     */
    public void unregisterSearch(Search search) {
        activeSearches.remove(search);
    }

    /**
     * Registers a search to this Menagerie so it will receive updates when items are modified.
     *
     * @param search Search to register.
     */
    public void registerSearch(Search search) {
        activeSearches.add(search);
    }

}
