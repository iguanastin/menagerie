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

    public GroupItem createGroup(List<Item> elements, String title) {
        if (title == null || title.isEmpty() || elements == null || elements.isEmpty()) return null;

        GroupItem group = new GroupItem(this, nextItemID, System.currentTimeMillis(), title);

        //TODO: Option to combine tags of elements for group

        for (Item item : elements) {
            if (item instanceof MediaItem && ((MediaItem) item).getGroup() == null) {
                group.addItem((MediaItem) item);
            } else if (item instanceof GroupItem) {
                removeItems(Collections.singletonList(item), false);
                ((GroupItem) item).getElements().forEach(group::addItem);
            } else {
                return null;
            }
        }

        nextItemID++;
        items.add(group);
        checkItemsStillValidInSearches(Collections.singletonList(group));

        //Tag with tagme
        Tag tagme = getTagByName("tagme");
        if (tagme == null) tagme = createTag("tagme");
        group.addTag(tagme);

        return group;
    }

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

    public List<Tag> getTags() {
        return tags;
    }

    private Tag getTagByID(int id) {
        for (Tag t : tags) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public Tag getTagByName(String name) {
        name = name.replace(' ', '_');
        for (Tag t : tags) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public void checkItemsStillValidInSearches(List<Item> items) {
        activeSearches.forEach(search -> search.recheckWithSearch(items));
    }

    public List<Item> getItems() {
        return items;
    }

    public DatabaseUpdater getDatabaseUpdater() {
        return databaseUpdater;
    }

    public Connection getDatabase() {
        return database;
    }

    private boolean isFilePresent(File file) {
        for (Item item : items) {
            if (item instanceof MediaItem && ((MediaItem) item).getFile().equals(file)) return true;
        }
        return false;
    }

    public void closeSearch(Search search) {
        activeSearches.remove(search);
    }

    public void registerSearch(Search search) {
        activeSearches.add(search);
    }

    public Tag createTag(String name) {
        name = name.replace(' ', '_');

        Tag t = new Tag(nextTagID, name);
        nextTagID++;

        tags.add(t);

        getDatabaseUpdater().createTagAsync(t.getId(), t.getName());

        return t;
    }

}
