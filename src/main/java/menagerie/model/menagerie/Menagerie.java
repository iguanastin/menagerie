package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
import menagerie.gui.Main;
import menagerie.model.db.DatabaseUpdateQueue;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.model.search.Search;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";

    // -------------------------------- SQL Statements ----------------------------

    private final PreparedStatement PS_GET_IMG_TAG_IDS;
    private final PreparedStatement PS_GET_HIGHEST_IMG_ID;
    private final PreparedStatement PS_GET_HIGHEST_TAG_ID;
    private final PreparedStatement PS_DELETE_IMG;
    private final PreparedStatement PS_CREATE_IMG;
    private final PreparedStatement PS_DELETE_TAG;
    private final PreparedStatement PS_CREATE_TAG;
    final PreparedStatement PS_SET_IMG_MD5;
    final PreparedStatement PS_SET_IMG_HISTOGRAM;
    final PreparedStatement PS_SET_IMG_THUMBNAIL;
    final PreparedStatement PS_GET_IMG_THUMBNAIL;
    final PreparedStatement PS_ADD_TAG_TO_IMG;
    final PreparedStatement PS_REMOVE_TAG_FROM_IMG;
    final PreparedStatement PS_SET_IMG_PATH;

    // ------------------------------ Variables -----------------------------------

    private final List<Item> items = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();

    private int nextImageID;
    private int nextTagID;

    private final Connection database;
    private final DatabaseUpdateQueue updateQueue = new DatabaseUpdateQueue();

    private final List<Search> activeSearches = new ArrayList<>();


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        //Initialize prepared database statements
        PS_GET_IMG_TAG_IDS = database.prepareStatement("SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;");
        PS_SET_IMG_MD5 = database.prepareStatement("UPDATE imgs SET imgs.md5=? WHERE imgs.id=?;");
        PS_SET_IMG_HISTOGRAM = database.prepareStatement("UPDATE imgs SET imgs.hist_a=?, imgs.hist_r=?, imgs.hist_g=?, imgs.hist_b=? WHERE imgs.id=?");
        PS_SET_IMG_THUMBNAIL = database.prepareStatement("UPDATE imgs SET imgs.thumbnail=? WHERE imgs.id=?;");
        PS_GET_IMG_THUMBNAIL = database.prepareStatement("SELECT imgs.thumbnail FROM imgs WHERE imgs.id=?;");
        PS_ADD_TAG_TO_IMG = database.prepareStatement("INSERT INTO tagged(img_id, tag_id) VALUES (?, ?);");
        PS_REMOVE_TAG_FROM_IMG = database.prepareStatement("DELETE FROM tagged WHERE img_id=? AND tag_id=?;");
        PS_GET_HIGHEST_IMG_ID = database.prepareStatement("SELECT TOP 1 imgs.id FROM imgs ORDER BY imgs.id DESC;");
        PS_GET_HIGHEST_TAG_ID = database.prepareStatement("SELECT TOP 1 tags.id FROM tags ORDER BY tags.id DESC;");
        PS_DELETE_IMG = database.prepareStatement("DELETE FROM imgs WHERE imgs.id=?;");
        PS_CREATE_IMG = database.prepareStatement("INSERT INTO imgs(id, path, added, md5, hist_a, hist_r, hist_g, hist_b) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        PS_DELETE_TAG = database.prepareStatement("DELETE FROM tags WHERE tags.id=?;");
        PS_CREATE_TAG = database.prepareStatement("INSERT INTO tags(id, name) VALUES (?, ?);");
        PS_SET_IMG_PATH = database.prepareStatement("UPDATE imgs SET path=? WHERE id=?;");

        // Load data from database
        loadTagsFromDatabase();
        loadImagesFromDatabase();
        clearUnusedTags();

        // Start runnable queue for database updates
        Thread thread = new Thread(updateQueue);
        thread.setDaemon(true);
        thread.start();

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
                System.out.println("Deleting unused tag: " + t);

                tags.remove(t);

                PS_DELETE_TAG.setInt(1, t.getId());
                PS_DELETE_TAG.executeUpdate();
            }
        }
    }

    private void initializeIdCounters() throws SQLException {
        ResultSet rs = PS_GET_HIGHEST_IMG_ID.executeQuery();
        if (rs.next()) {
            nextImageID = rs.getInt("id") + 1;
        } else {
            nextImageID = 1;
        }
        rs.close();

        rs = PS_GET_HIGHEST_TAG_ID.executeQuery();
        if (rs.next()) {
            nextTagID = rs.getInt("id") + 1;
        } else {
            nextTagID = 1;
        }
        rs.close();
    }

    private void loadImagesFromDatabase() throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_IMGS);

        while (rs.next()) {
            ImageHistogram hist = null;

            InputStream histAlpha = rs.getBinaryStream("hist_a");
            if (histAlpha != null) {
                try {
                    hist = new ImageHistogram(histAlpha, rs.getBinaryStream("hist_r"), rs.getBinaryStream("hist_g"), rs.getBinaryStream("hist_b"));
                } catch (HistogramReadException e) {
                    System.out.println("Histogram failed to load from database:");
                    e.printStackTrace();
                }
            }

            MediaItem img = new MediaItem(this, rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")), rs.getNString("md5"), hist);
            items.add(img);

            PS_GET_IMG_TAG_IDS.setInt(1, img.getId());
            ResultSet tagRS = PS_GET_IMG_TAG_IDS.executeQuery();

            while (tagRS.next()) {
                Tag tag = getTagByID(tagRS.getInt("tag_id"));
                if (tag != null) {
                    tag.incrementFrequency();
                    img.getTags().add(tag);
                } else {
                    System.err.println("Major issue, tag wasn't loaded in but somehow still exists in the database");
                }
            }
        }

        s.close();

        System.out.println("Finished loading " + items.size() + " images from database");
    }

    private void loadTagsFromDatabase() throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_TAGS);

        while (rs.next()) {
            tags.add(new Tag(rs.getInt("id"), rs.getNString("name")));
        }

        s.close();

        System.out.println("Finished loading " + tags.size() + " tags from database");
    }

    public MediaItem importFile(File file) {
        if (isFilePresent(file)) return null;

        MediaItem media = new MediaItem(this, nextImageID, System.currentTimeMillis(), file, null, null);

        //Add image and commit to database
        items.add(media);
        nextImageID++;
        try {
            PS_CREATE_IMG.setInt(1, media.getId());
            PS_CREATE_IMG.setNString(2, media.getFile().getAbsolutePath());
            PS_CREATE_IMG.setLong(3, media.getDateAdded());
            PS_CREATE_IMG.setNString(4, media.getMD5());
            PS_CREATE_IMG.setBinaryStream(5, null);
            PS_CREATE_IMG.setBinaryStream(6, null);
            PS_CREATE_IMG.setBinaryStream(7, null);
            PS_CREATE_IMG.setBinaryStream(8, null);
            if (media.getHistogram() != null) {
                PS_CREATE_IMG.setBinaryStream(5, media.getHistogram().getAlphaAsInputStream());
                PS_CREATE_IMG.setBinaryStream(6, media.getHistogram().getRedAsInputStream());
                PS_CREATE_IMG.setBinaryStream(7, media.getHistogram().getGreenAsInputStream());
                PS_CREATE_IMG.setBinaryStream(8, media.getHistogram().getBlueAsInputStream());
            }
            PS_CREATE_IMG.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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

        GroupItem group = new GroupItem(this, nextImageID, System.currentTimeMillis(), title);

        for (Item item : elements) {
            if (item instanceof MediaItem && ((MediaItem) item).getGroup() == null) {
                group.addItem((MediaItem) item);
            } else {
                return null;
            }
        }

        nextImageID++;
        items.add(group);
        checkItemsStillValidInSearches(Collections.singletonList(group));

        //Tag with tagme
        Tag tagme = getTagByName("tagme");
        if (tagme == null) tagme = createTag("tagme");
        group.addTag(tagme);

        return group;
    }

    public void removeImages(List<Item> items, boolean deleteFiles) {
        List<Item> removed = new ArrayList<>();
        List<MediaItem> toDelete = new ArrayList<>();

        for (Item item : items) {
            if (getItems().remove(item)) {
                item.getTags().forEach(Tag::decrementFrequency);

                removed.add(item);
                if (deleteFiles && item instanceof MediaItem) toDelete.add((MediaItem) item);
                if (item instanceof GroupItem) ((GroupItem) item).removeAll();

                updateQueue.enqueueUpdate(() -> {
                    try {
                        PS_DELETE_IMG.setInt(1, item.getId());
                        PS_DELETE_IMG.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
                updateQueue.commit();
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

    public DatabaseUpdateQueue getUpdateQueue() {
        return updateQueue;
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
        Tag t = new Tag(nextTagID, name);
        nextTagID++;

        tags.add(t);

        updateQueue.enqueueUpdate(() -> {
            try {
                PS_CREATE_TAG.setInt(1, t.getId());
                PS_CREATE_TAG.setNString(2, t.getName());
                PS_CREATE_TAG.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return t;
    }
}
