package menagerie.model.menagerie;

import javafx.application.Platform;
import menagerie.gui.Main;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.search.Search;

import java.io.File;
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
    public MediaItem importFile(File file) {
        if (isFilePresent(file)) return null;

        MediaItem media = new MediaItem(this, nextItemID, System.currentTimeMillis(), file);

        // Add media and commit to database
        items.add(media);
        nextItemID++;
        try {
            getDatabaseManager().createMedia(media);
        } catch (SQLException e) {
            Main.log.log(Level.SEVERE, "Failed to create media in database: " + media, e);
            return null;
        }

        //Update active searches
        refreshInSearches(media);

        return media;
    }

    /**
     * Creates a group and adds elements to it.
     *
     * @param elements Elements to be added. If an element is a GroupItem, that group's contents will be removed and added to the new group.
     * @param title    Title of the new group.
     * @return The newly created group. Null if title is null or empty, and null if element list is null or empty.
     */
    public GroupItem createGroup(List<Item> elements, String title) {
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
                item.getTags().forEach(group::addTag);
                forgetItem(item);
                e.forEach(group::addItem);
            } else {
                return null;
            }
        }

        // Update searches
        refreshInSearches(group);

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
     * Forgets a set of items.
     *
     * @param items Items
     */
    public void forgetItems(List<Item> items) {
        items.forEach(Item::forget);

        refreshInSearches(items);
    }

    /**
     * Forgets an item.
     *
     * @param item Item
     */
    public void forgetItem(Item item) {
        item.forget();

        refreshInSearches(item);
    }

    /**
     * Deletes a set of items.
     *
     * @param items Items
     */
    public void deleteItems(List<Item> items) {
        items.forEach(Item::delete);

        refreshInSearches(items);
    }

    /**
     * Deletes an item.
     *
     * @param item Item
     */
    public void deleteItem(Item item) {
        item.delete();

        refreshInSearches(item);
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
     * Adds items to any searches that they are valid in, removes them from any searches they are not valid in.
     *
     * @param items Items to check.
     */
    public void refreshInSearches(List<Item> items) {
        activeSearches.forEach(search -> Platform.runLater(() -> search.refreshSearch(items)));
    }

    /**
     * Adds the item to any searches that it is valid in, removes it from any searches it is not valid in.
     *
     * @param item Item to check.
     */
    public void refreshInSearches(Item item) {
        refreshInSearches(Collections.singletonList(item));
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
    public boolean isFilePresent(File file) {
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
