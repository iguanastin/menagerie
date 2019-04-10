package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
import menagerie.gui.Main;
import menagerie.model.menagerie.db.DatabaseUpdater;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.model.search.Search;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Menagerie system. Contains items, manages database.
 */
public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";
    private static final String SQL_GET_IMG_TAGS = "SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;";
    private static final String SQL_GET_HIGHEST_IMG_ID = "SELECT TOP 1 imgs.id FROM imgs ORDER BY imgs.id DESC;";
    private static final String SQL_GET_HIGHEST_TAG_ID = "SELECT TOP 1 tags.id FROM tags ORDER BY tags.id DESC;";

    // ------------------------------ Variables -----------------------------------

    private final List<Item> items = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();

    private int nextItemID;
    private int nextTagID;

    private final Connection database;
    private final DatabaseUpdater databaseUpdater;

    private final List<Search> activeSearches = new ArrayList<>();


    /**
     * Constructs a Menagerie. Starts a database updater thread, loads tags and media info from database, prunes database.
     *
     * @param database Database to back this menagerie. Expected to be in the current schema.
     * @throws SQLException If any errors occur in database loading/prep.
     */
    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        databaseUpdater = new DatabaseUpdater(database);
        databaseUpdater.setDaemon(true);
        databaseUpdater.start();

        // Load data from database
        loadTagsFromDatabase();
        loadMediaFromDatabase();
        clearUnusedTags();

        initializeIdCounters();
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
                getDatabaseUpdater().deleteTag(t.getId());
            }
        }
    }

    /**
     * Initializes ID counters used for creating new items and tags.
     *
     * @throws SQLException If any error occurs in the database.
     */
    private void initializeIdCounters() throws SQLException {
        try (Statement s = database.createStatement()) {
            try (ResultSet rs = s.executeQuery(SQL_GET_HIGHEST_IMG_ID)) {
                if (rs.next()) {
                    nextItemID = rs.getInt("id") + 1;
                } else {
                    nextItemID = 1;
                }
                rs.close();

                try (ResultSet rs2 = s.executeQuery(SQL_GET_HIGHEST_TAG_ID)) {
                    if (rs2.next()) {
                        nextTagID = rs2.getInt("id") + 1;
                    } else {
                        nextTagID = 1;
                    }
                }
            }
        }
    }

    /**
     * Loads all media items from the database into MediaItems.
     *
     * @throws SQLException If any error occurs in the database.
     */
    private void loadMediaFromDatabase() throws SQLException {
        try (Statement s = database.createStatement()) {
            try (ResultSet rs = s.executeQuery(SQL_GET_IMGS)) {
                while (rs.next()) {
                    ImageHistogram hist = null;

                    InputStream histAlpha = rs.getBinaryStream("hist_a");
                    if (histAlpha != null) {
                        try {
                            hist = new ImageHistogram(histAlpha, rs.getBinaryStream("hist_r"), rs.getBinaryStream("hist_g"), rs.getBinaryStream("hist_b"));
                        } catch (HistogramReadException e) {
                            Main.log.log(Level.SEVERE, "Histogram failed to load from database", e);
                        }
                    }

                    MediaItem media = new MediaItem(this, rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")), rs.getNString("md5"), hist);
                    items.add(media);

                    PreparedStatement ps = database.prepareStatement(SQL_GET_IMG_TAGS);
                    ps.setInt(1, media.getId());
                    ResultSet tagRS = ps.executeQuery();

                    while (tagRS.next()) {
                        Tag tag = getTagByID(tagRS.getInt("tag_id"));
                        if (tag != null) {
                            tag.incrementFrequency();
                            media.getTags().add(tag);
                        } else {
                            Main.log.warning("Major issue, tag wasn't loaded in but somehow still exists in the database: " + tagRS.getInt("tag_id"));
                        }
                    }

                    ps.close();
                }

                Main.log.info("Finished loading " + items.size() + " images from database");
            }
        }
    }

    /**
     * Loads all tags from the database.
     *
     * @throws SQLException If any error occurs in the database.
     */
    private void loadTagsFromDatabase() throws SQLException {
        try (Statement s = database.createStatement()) {
            try (ResultSet rs = s.executeQuery(SQL_GET_TAGS)) {
                while (rs.next()) {
                    tags.add(new Tag(rs.getInt("id"), rs.getNString("name")));
                }

                Main.log.info("Finished loading " + tags.size() + " tags from database");
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

        MediaItem media = new MediaItem(this, nextItemID, System.currentTimeMillis(), file, null, null);

        //Add image and commit to database
        items.add(media);
        nextItemID++;
        try {
            getDatabaseUpdater().createMedia(media);
        } catch (SQLException e) {
            Main.log.log(Level.SEVERE, "Failed to create media in database: " + media, e);
        }

        //Tag with tagme
        Tag tagme = getTagByName("tagme");
        if (tagme == null) tagme = createTag("tagme");
        media.addTag(tagme);

        if (media.isVideo()) {
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
    public GroupItem createGroup(List<Item> elements, String title) {
        if (title == null || title.isEmpty() || elements == null || elements.isEmpty()) return null;

        GroupItem group = new GroupItem(this, nextItemID, System.currentTimeMillis(), title);

        //TODO: Option to combine tags of elements for group

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

        nextItemID++;
        items.add(group);
        checkItemsStillValidInSearches(Collections.singletonList(group));

        // TODO: Store new group in database

        //Tag with tagme
        Tag tagme = getTagByName("tagme");
        if (tagme == null) tagme = createTag("tagme");
        group.addTag(tagme);

        return group;
    }

    /**
     * Creates a new tag in this Menagerie.
     *
     * @param name Name of new tag. Must be unique (case insensitive).
     * @return The newly created tag, or null if name is not unique.
     */
    public Tag createTag(String name) {
        Tag t = new Tag(nextTagID, name);
        nextTagID++;

        tags.add(t);

        getDatabaseUpdater().createTagAsync(t.getId(), t.getName());

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

                getDatabaseUpdater().removeItemAsync(item.getId());
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
                    //TODO: better error handling, preferably send to error list in gui
                    Main.showErrorMessage("Recycle bin Error", "Unable to send files to recycle bin", toDelete.size() + "");
                }
            } else {
                toDelete.forEach(item -> {
                    if (item.getFile().delete()) {
                        //TODO: better error handling, preferably send to error list in gui
                        Main.showErrorMessage("Deletion Error", "Unable to delete file", item.getFile().toString());
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
    private Tag getTagByID(int id) {
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
     *
     * @return The database updater thread backing this Menagerie.
     */
    public DatabaseUpdater getDatabaseUpdater() {
        return databaseUpdater;
    }

    /**
     *
     * @return The database backing this menagerie.
     */
    public Connection getDatabase() {
        return database;
    }

    /**
     *
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
