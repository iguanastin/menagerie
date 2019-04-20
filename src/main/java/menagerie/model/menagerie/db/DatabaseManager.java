package menagerie.model.menagerie.db;

import javafx.scene.image.Image;
import menagerie.gui.Main;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.menagerie.*;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.util.ImageInputStreamConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Menagerie database updater thread. Provides methods for synchronous database updates as well as asynchronous updates.
 */
public class DatabaseManager extends Thread {

    // Media
    private final PreparedStatement PS_CREATE_MEDIA;
    private final PreparedStatement PS_SET_MEDIA_GID;
    private final PreparedStatement PS_SET_MEDIA_MD5;
    private final PreparedStatement PS_SET_MEDIA_PATH;
    private final PreparedStatement PS_SET_MEDIA_HISTOGRAM;
    private final PreparedStatement PS_SET_MEDIA_PAGE;
    private final PreparedStatement PS_SET_MEDIA_THUMBNAIL;
    private final PreparedStatement PS_GET_MEDIA_THUMBNAIL;
    // Groups
    private final PreparedStatement PS_CREATE_GROUP;
    private final PreparedStatement PS_SET_GROUP_TITLE;
    // Items
    private final PreparedStatement PS_CREATE_ITEM;
    private final PreparedStatement PS_DELETE_ITEM;
    // Tags
    private final PreparedStatement PS_DELETE_TAG;
    private final PreparedStatement PS_CREATE_TAG;
    private final PreparedStatement PS_ADD_TAG_TO_ITEM;
    private final PreparedStatement PS_REMOVE_TAG_FROM_ITEM;
    private final PreparedStatement PS_ADD_TAG_NOTE;
    private final PreparedStatement PS_REMOVE_TAG_NOTE;
    private final PreparedStatement PS_SET_TAG_COLOR;
    // Counters
    private final PreparedStatement PS_GET_HIGHEST_ITEM_ID;
    private final PreparedStatement PS_GET_HIGHEST_TAG_ID;
    // Construction
    private final PreparedStatement PS_GET_TAGS;
    private final PreparedStatement PS_GET_MEDIA;
    private final PreparedStatement PS_GET_GROUPS;
    private final PreparedStatement PS_GET_TAGS_FOR_ITEM;
    private final PreparedStatement PS_GET_TAG_NOTES;
    // Teardown
    private final PreparedStatement PS_SHUTDOWN_DEFRAG;

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private final Timer loggingTimer = new Timer(true);
    private final Lock loggingLock = new ReentrantLock();
    private int databaseUpdates = 0;
    private long lastLog = System.currentTimeMillis();


    public DatabaseManager(Connection database) throws SQLException {

        // ------------------------------------ Init statements -----------------------------------
        // Media
        PS_CREATE_MEDIA = database.prepareStatement("INSERT INTO media(id, path, md5, hist_a, hist_r, hist_g, hist_b) VALUES (?, ?, ?, ?, ?, ?, ?);");
        PS_SET_MEDIA_GID = database.prepareStatement("UPDATE media SET gid=? WHERE id=?;");
        PS_SET_MEDIA_MD5 = database.prepareStatement("UPDATE media SET md5=? WHERE id=?;");
        PS_SET_MEDIA_PATH = database.prepareStatement("UPDATE media SET path=? WHERE id=?;");
        PS_SET_MEDIA_HISTOGRAM = database.prepareStatement("UPDATE media SET hist_a=?, hist_r=?, hist_g=?, hist_b=? WHERE id=?");
        PS_SET_MEDIA_PAGE = database.prepareStatement("UPDATE media SET page=? WHERE id=?;");
        PS_SET_MEDIA_THUMBNAIL = database.prepareStatement("UPDATE media SET thumbnail=? WHERE id=?;");
        PS_GET_MEDIA_THUMBNAIL = database.prepareStatement("SELECT thumbnail FROM media WHERE id=?;");
        // Groups
        PS_CREATE_GROUP = database.prepareStatement("INSERT INTO groups(id, title) VALUES (?, ?);");
        PS_SET_GROUP_TITLE = database.prepareStatement("UPDATE groups SET title=? WHERE id=?;");
        // Items
        PS_DELETE_ITEM = database.prepareStatement("DELETE FROM items WHERE id=?;");
        PS_CREATE_ITEM = database.prepareStatement("INSERT INTO items(id, added) VALUES (?, ?);");
        // Tags
        PS_DELETE_TAG = database.prepareStatement("DELETE FROM tags WHERE id=?;");
        PS_CREATE_TAG = database.prepareStatement("INSERT INTO tags(id, name) VALUES (?, ?);");
        PS_ADD_TAG_TO_ITEM = database.prepareStatement("INSERT INTO tagged(item_id, tag_id) VALUES (?, ?);");
        PS_REMOVE_TAG_FROM_ITEM = database.prepareStatement("DELETE FROM tagged WHERE item_id=? AND tag_id=?;");
        PS_ADD_TAG_NOTE = database.prepareStatement("INSERT INTO tag_notes(tag_id, note) VALUES (?, ?);");
        PS_REMOVE_TAG_NOTE = database.prepareStatement("DELETE TOP 1 FROM tag_notes WHERE tag_id=? AND note LIKE ?;");
        PS_SET_TAG_COLOR = database.prepareStatement("UPDATE tags SET color=? WHERE id=?;");
        // Counters
        PS_GET_HIGHEST_ITEM_ID = database.prepareStatement("SELECT TOP 1 id FROM items ORDER BY id DESC;");
        PS_GET_HIGHEST_TAG_ID = database.prepareStatement("SELECT TOP 1 id FROM tags ORDER BY id DESC;");
        // Construction
        PS_GET_TAGS = database.prepareStatement("SELECT * FROM tags;");
        PS_GET_MEDIA = database.prepareStatement("SELECT items.id, items.added, media.gid, media.page, media.path, media.md5, media.hist_a, media.hist_r, media.hist_g, media.hist_b FROM media JOIN items ON items.id=media.id;");
        PS_GET_GROUPS = database.prepareStatement("SELECT items.id, items.added, groups.title FROM groups JOIN items ON items.id=groups.id;");
        PS_GET_TAGS_FOR_ITEM = database.prepareStatement("SELECT tagged.tag_id FROM tagged WHERE tagged.item_id=?;");
        PS_GET_TAG_NOTES = database.prepareStatement("SELECT * FROM tag_notes;");
        // Teardown
        PS_SHUTDOWN_DEFRAG = database.prepareStatement("SHUTDOWN DEFRAG;");
    }

    @Override
    public void run() {
        running = true;

        startLoggingTimer();

        while (running) {
            try {
                Runnable job = queue.take();
                try {
                    loggingLock.lock();
                    databaseUpdates++;
                } finally {
                    loggingLock.unlock();
                }

                try {
                    job.run();
                } catch (Exception e) {
                    Main.log.log(Level.SEVERE, "Exception while running database updater job", e);
                }
            } catch (InterruptedException e) {
                Main.log.log(Level.WARNING, "Database updater interrupted while waiting for queue", e);
            }
        }
    }

    /**
     * Initializes the logging timer that outputs update counts regularly.
     */
    private void startLoggingTimer() {
        loggingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    loggingLock.lock();
                    if (databaseUpdates > 0) {
                        Main.log.info(String.format("DatabaseManager updated %d times in the last %.2fm", databaseUpdates, (System.currentTimeMillis() - lastLog) / 1000.0 / 60.0));
                        lastLog = System.currentTimeMillis();
                        databaseUpdates = 0;
                    }
                } finally {
                    loggingLock.unlock();
                }
            }
        }, 60000, 60000);
    }

    /**
     * Enqueues a job to this thread. FIFO.
     *
     * @param job Job to enqueue.
     */
    public void enqueue(Runnable job) {
        queue.add(job);
    }

    /**
     * Stores a thumbnail in the database.
     *
     * @param id        ID of item to update.
     * @param thumbnail Thumbnail to store.
     * @throws SQLException If the database update failed.
     * @throws IOException  If the thumbnail could not be converted to a stream.
     */
    public void setThumbnail(int id, Image thumbnail) throws SQLException, IOException {
        synchronized (PS_SET_MEDIA_THUMBNAIL) {
            PS_SET_MEDIA_THUMBNAIL.setBinaryStream(1, ImageInputStreamConverter.imageToInputStream(thumbnail));
            PS_SET_MEDIA_THUMBNAIL.setInt(2, id);
            PS_SET_MEDIA_THUMBNAIL.executeUpdate();
        }
    }

    /**
     * Queues a thumbnail to be stored in the database.
     *
     * @param id        ID of item to update.
     * @param thumbnail Thumbnail to store.
     */
    public void setThumbnailAsync(int id, Image thumbnail) {
        queue.add(() -> {
            try {
                setThumbnail(id, thumbnail);
            } catch (SQLException | IOException e) {
                Main.log.log(Level.SEVERE, "Failed to set thumbnail async: " + id, e);
            }
        });
    }

    /**
     * Stores an MD5 string in the database.
     *
     * @param id  ID of item to update.
     * @param md5 MD5 to store.
     * @throws SQLException If the database update failed.
     */
    public void setMD5(int id, String md5) throws SQLException {
        synchronized (PS_SET_MEDIA_MD5) {
            PS_SET_MEDIA_MD5.setNString(1, md5);
            PS_SET_MEDIA_MD5.setInt(2, id);
            PS_SET_MEDIA_MD5.executeUpdate();
        }
    }

    /**
     * Queues an MD5 string to be stored in the database.
     *
     * @param id  ID of item to update.
     * @param md5 MD5 to store.
     */
    public void setMD5Async(int id, String md5) {
        queue.add(() -> {
            try {
                setMD5(id, md5);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set md5 async: " + id + " - md5: " + md5, e);
            }
        });
    }

    /**
     * Stores a histogram in the database.
     *
     * @param id   ID of item to update.
     * @param hist Histogram to store.
     * @throws SQLException If database update fails.
     */
    public void setHist(int id, ImageHistogram hist) throws SQLException {
        synchronized (PS_SET_MEDIA_HISTOGRAM) {
            PS_SET_MEDIA_HISTOGRAM.setBinaryStream(1, hist.getAlphaAsInputStream());
            PS_SET_MEDIA_HISTOGRAM.setBinaryStream(2, hist.getRedAsInputStream());
            PS_SET_MEDIA_HISTOGRAM.setBinaryStream(3, hist.getGreenAsInputStream());
            PS_SET_MEDIA_HISTOGRAM.setBinaryStream(4, hist.getBlueAsInputStream());
            PS_SET_MEDIA_HISTOGRAM.setInt(5, id);
            PS_SET_MEDIA_HISTOGRAM.executeUpdate();
        }
    }

    /**
     * Queues a histogram to be stored in the database.
     *
     * @param id   ID of item to update.
     * @param hist Histogram to store.
     */
    public void setHistAsync(int id, ImageHistogram hist) {
        queue.add(() -> {
            try {
                setHist(id, hist);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set histogram async: " + id, e);
            }
        });
    }

    /**
     * Stores a path in the database.
     *
     * @param id   ID of item to update.
     * @param path Path to store.
     * @throws SQLException If database update fails.
     */
    public void setPath(int id, String path) throws SQLException {
        synchronized (PS_SET_MEDIA_PATH) {
            PS_SET_MEDIA_PATH.setNString(1, path);
            PS_SET_MEDIA_PATH.setInt(2, id);
            PS_SET_MEDIA_PATH.executeUpdate();
        }
    }

    /**
     * Queues a path to be stored in the database.
     *
     * @param id   ID of item to update.
     * @param path Path to store.
     */
    public void setPathAsync(int id, String path) {
        queue.add(() -> {
            try {
                setPath(id, path);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set new path: " + id + " - \"" + path + "\"", e);
            }
        });
    }

    /**
     * Tags an item with a tag.
     *
     * @param item ID of item.
     * @param tag  ID of tag.
     * @throws SQLException If database update fails.
     */
    public void tagItem(int item, int tag) throws SQLException {
        synchronized (PS_ADD_TAG_TO_ITEM) {
            PS_ADD_TAG_TO_ITEM.setInt(1, item);
            PS_ADD_TAG_TO_ITEM.setInt(2, tag);
            PS_ADD_TAG_TO_ITEM.executeUpdate();
        }
    }

    /**
     * Queues a tag to be added to an item.
     *
     * @param item ID of item.
     * @param tag  ID of tag.
     */
    public void tagItemAsync(int item, int tag) {
        queue.add(() -> {
            try {
                tagItem(item, tag);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to tag item: " + item + " with tag: " + tag, e);
            }
        });
    }

    /**
     * Removes a tag from an item.
     *
     * @param item ID of item.
     * @param tag  ID of tag.
     * @throws SQLException If database update fails.
     */
    public void untagItem(int item, int tag) throws SQLException {
        synchronized (PS_REMOVE_TAG_FROM_ITEM) {
            PS_REMOVE_TAG_FROM_ITEM.setInt(1, item);
            PS_REMOVE_TAG_FROM_ITEM.setInt(2, tag);
            PS_REMOVE_TAG_FROM_ITEM.executeUpdate();
        }
    }

    /**
     * Queues a tag to be removed from an item.
     *
     * @param item ID of item.
     * @param tag  ID of tag.
     */
    public void untagItemAsync(int item, int tag) {
        queue.add(() -> {
            try {
                untagItem(item, tag);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to untag item: " + item + " from tag: " + tag, e);
            }
        });
    }

    /**
     * Removes an item from the database.
     *
     * @param id ID of item.
     * @throws SQLException If database update fails.
     */
    public void removeItem(int id) throws SQLException {
        synchronized (PS_DELETE_ITEM) {
            PS_DELETE_ITEM.setInt(1, id);
            PS_DELETE_ITEM.executeUpdate();
        }
    }

    /**
     * Queues an item to be removed from the database.
     *
     * @param id ID of item.
     */
    public void removeItemAsync(int id) {
        queue.add(() -> {
            try {
                removeItem(id);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to remove item: " + id, e);
            }
        });
    }

    /**
     * Creates a tag in the database.
     *
     * @param id   ID of tag.
     * @param name Name of tag.
     * @throws SQLException If database update fails.
     */
    public void createTag(int id, String name) throws SQLException {
        synchronized (PS_CREATE_TAG) {
            PS_CREATE_TAG.setInt(1, id);
            PS_CREATE_TAG.setNString(2, name);
            PS_CREATE_TAG.executeUpdate();
        }
    }

    /**
     * Queues a tag to be created in the database.
     *
     * @param id   ID of tag.
     * @param name Name of tag.
     */
    public void createTagAsync(int id, String name) {
        queue.add(() -> {
            try {
                createTag(id, name);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to create tag: " + id + " - \"" + name + "\"", e);
            }
        });
    }

    /**
     * Deletes a tag from the database.
     *
     * @param id ID of tag.
     * @throws SQLException If database update fails.
     */
    public void deleteTag(int id) throws SQLException {
        synchronized (PS_DELETE_TAG) {
            PS_DELETE_TAG.setInt(1, id);
            PS_DELETE_TAG.executeUpdate();
        }
    }

    /**
     * Queues a tag to be deleted from the database.
     *
     * @param id ID of tag.
     */
    public void deleteTagAsync(int id) {
        queue.add(() -> {
            try {
                deleteTag(id);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to delete tag: " + id, e);
            }
        });
    }

    /**
     * Stores a new MediaItem in the database.
     *
     * @param media Item to store.
     * @throws SQLException If database update fails.
     */
    public void createMedia(MediaItem media) throws SQLException {
        synchronized (PS_CREATE_ITEM) {
            PS_CREATE_ITEM.setInt(1, media.getId());
            PS_CREATE_ITEM.setLong(2, media.getDateAdded());
            PS_CREATE_ITEM.executeUpdate();
        }
        synchronized (PS_CREATE_MEDIA) {
            PS_CREATE_MEDIA.setInt(1, media.getId());
            PS_CREATE_MEDIA.setNString(2, media.getFile().getAbsolutePath());
            PS_CREATE_MEDIA.setNString(3, media.getMD5());
            PS_CREATE_MEDIA.setBinaryStream(4, null);
            PS_CREATE_MEDIA.setBinaryStream(5, null);
            PS_CREATE_MEDIA.setBinaryStream(6, null);
            PS_CREATE_MEDIA.setBinaryStream(7, null);
            if (media.getHistogram() != null) {
                PS_CREATE_MEDIA.setBinaryStream(4, media.getHistogram().getAlphaAsInputStream());
                PS_CREATE_MEDIA.setBinaryStream(5, media.getHistogram().getRedAsInputStream());
                PS_CREATE_MEDIA.setBinaryStream(6, media.getHistogram().getGreenAsInputStream());
                PS_CREATE_MEDIA.setBinaryStream(7, media.getHistogram().getBlueAsInputStream());
            }
            PS_CREATE_MEDIA.executeUpdate();
        }
    }

    /**
     * Queues a MediaItem to be stored in the database.
     *
     * @param media Item to store.
     */
    public void createMediaAsync(MediaItem media) {
        queue.add(() -> {
            try {
                createMedia(media);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to create media async: " + media, e);
            }
        });
    }

    /**
     * Stores a new GroupItem in the database.
     *
     * @param group Group to store.
     * @throws SQLException If database update fails.
     */
    public void createGroup(GroupItem group) throws SQLException {
        synchronized (PS_CREATE_ITEM) {
            PS_CREATE_ITEM.setInt(1, group.getId());
            PS_CREATE_ITEM.setLong(2, group.getDateAdded());
            PS_CREATE_ITEM.executeUpdate();
        }
        synchronized (PS_CREATE_GROUP) {
            PS_CREATE_GROUP.setInt(1, group.getId());
            PS_CREATE_GROUP.setNString(2, group.getTitle());
            PS_CREATE_GROUP.executeUpdate();
        }
    }

    /**
     * Queues a GroupItem to be stored in the database.
     *
     * @param group Group to store.
     */
    public void createGroupAsync(GroupItem group) {
        queue.add(() -> {
            try {
                createGroup(group);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to create group async: " + group, e);
            }
        });
    }

    /**
     * Sets the GID (parent group ID) of a media item.
     *
     * @param id  ID of media.
     * @param gid ID of group.
     * @throws SQLException When database update fails.
     */
    public void setMediaGID(int id, Integer gid) throws SQLException {
        synchronized (PS_SET_MEDIA_GID) {
            if (gid == null) {
                PS_SET_MEDIA_GID.setNull(1, Types.INTEGER);
            } else {
                PS_SET_MEDIA_GID.setInt(1, gid);
            }
            PS_SET_MEDIA_GID.setInt(2, id);
            PS_SET_MEDIA_GID.executeUpdate();
        }
    }

    /**
     * Queues an update to set the GID of a media item.
     *
     * @param id  ID of media.
     * @param gid ID of group.
     */
    public void setMediaGIDAsync(int id, Integer gid) {
        queue.add(() -> {
            try {
                setMediaGID(id, gid);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, String.format("Failed to set media GID async. ID: %d, GID: %d", id, gid), e);
            }
        });
    }

    /**
     * Sets the page index of a media item.
     *
     * @param id   ID of media item.
     * @param page Page index to set.
     * @throws SQLException If database update fails.
     */
    public void setMediaPage(int id, int page) throws SQLException {
        synchronized (PS_SET_MEDIA_PAGE) {
            PS_SET_MEDIA_PAGE.setInt(1, page);
            PS_SET_MEDIA_PAGE.setInt(2, id);
            PS_SET_MEDIA_PAGE.executeUpdate();
        }
    }

    /**
     * Queues an update to set the page index of a media item.
     *
     * @param id   ID of media item.
     * @param page Page index to set.
     */
    public void setMediaPageAsync(int id, int page) {
        queue.add(() -> {
            try {
                setMediaPage(id, page);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, String.format("Failed to set media page index. ID: %d, Page: %d", id, page), e);
            }
        });
    }

    /**
     * Sets the title of a group.
     *
     * @param id    ID of group.
     * @param title Title to set to.
     */
    public void setGroupTitle(int id, String title) throws SQLException {
        synchronized (PS_SET_GROUP_TITLE) {
            PS_SET_GROUP_TITLE.setNString(1, title);
            PS_SET_GROUP_TITLE.setInt(2, id);
            PS_SET_GROUP_TITLE.executeUpdate();
        }
    }

    /**
     * Queues an update to set the title of a group.
     *
     * @param id    ID of group.
     * @param title Title to set.
     */
    public void setGroupTitleAsync(int id, String title) {
        queue.add(() -> {
            try {
                setGroupTitle(id, title);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set group title. ID: " + id + ", Title: " + title, e);
            }
        });
    }

    /**
     * Inserts a note into the tag_notes table.
     *
     * @param id   ID of tag note is attached to.
     * @param note The note.
     * @throws SQLException When database update fails.
     */
    public void addTagNote(int id, String note) throws SQLException {
        synchronized (PS_ADD_TAG_NOTE) {
            PS_ADD_TAG_NOTE.setInt(1, id);
            PS_ADD_TAG_NOTE.setNString(2, note);
            PS_ADD_TAG_NOTE.executeUpdate();
        }
    }

    /**
     * Queues a note to be inserted into the tag_notes table.
     *
     * @param id   ID of tag note is attached to.
     * @param note The note.
     */
    public void addTagNoteAsync(int id, String note) {
        queue.add(() -> {
            try {
                addTagNote(id, note);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, String.format("Failed to insert tag note. Tag ID: %d, Note: \"%s\"", id, note), e);
            }
        });
    }

    /**
     * Deletes a note from the tag_notes table.
     *
     * @param id   ID of tag note is attached to.
     * @param note The note.
     * @throws SQLException If database update fails.
     */
    public void removeTagNote(int id, String note) throws SQLException {
        synchronized (PS_REMOVE_TAG_NOTE) {
            PS_REMOVE_TAG_NOTE.setInt(1, id);
            PS_REMOVE_TAG_NOTE.setNString(2, note);
            PS_REMOVE_TAG_NOTE.executeUpdate();
        }
    }

    /**
     * Queues a tag note to be deleted.
     *
     * @param id   ID of tag.
     * @param note The note.
     */
    public void removeTagNoteAsync(int id, String note) {
        queue.add(() -> {
            try {
                removeTagNote(id, note);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, String.format("Failed to remove tag note. Tag ID: %d, Note: \"%s\"", id, note), e);
            }
        });
    }

    /**
     * Sets the color of a tag.
     *
     * @param id    ID of tag.
     * @param color Color to set.
     * @throws SQLException If database update fails.
     */
    public void setTagColor(int id, String color) throws SQLException {
        synchronized (PS_SET_TAG_COLOR) {
            PS_SET_TAG_COLOR.setNString(1, color);
            PS_SET_TAG_COLOR.setInt(2, id);
            PS_SET_TAG_COLOR.executeUpdate();
        }
    }

    /**
     * Queues a color to be set for a tag.
     *
     * @param id    ID of tag to update.
     * @param color Color to set.
     */
    public void setTagColorAsync(int id, String color) {
        queue.add(() -> {
            try {
                setTagColor(id, color);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, String.format("Failed to set tag color: ID: %d, Color: %s", id, color), e);
            }
        });
    }

    /**
     * Gets a thumbnail from the database.
     *
     * @param id ID of item.
     * @return Thumbnail of the item, or null if there is no thumbnail.
     * @throws SQLException If database query fails.
     */
    public Thumbnail getThumbnail(int id) throws SQLException {
        synchronized (PS_GET_MEDIA_THUMBNAIL) {
            PS_GET_MEDIA_THUMBNAIL.setInt(1, id);
            try (ResultSet rs = PS_GET_MEDIA_THUMBNAIL.executeQuery()) {
                if (rs.next()) {
                    InputStream binaryStream = rs.getBinaryStream(1);
                    if (binaryStream != null) {
                        return new Thumbnail(ImageInputStreamConverter.imageFromInputStream(binaryStream));
                    }
                }
            }
        }

        return null;
    }

    /**
     * Finds the highest item ID value.
     *
     * @return Highest ID value, or 0 if none.
     * @throws SQLException If database query fails.
     */
    public int getHighestItemID() throws SQLException {
        synchronized (PS_GET_HIGHEST_ITEM_ID) {
            try (ResultSet rs = PS_GET_HIGHEST_ITEM_ID.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Finds the highest tag ID value.
     *
     * @return Highest ID value, or 0 if none.
     * @throws SQLException If database query fails.
     */
    public int getHighestTagID() throws SQLException {
        synchronized (PS_GET_HIGHEST_TAG_ID) {
            try (ResultSet rs = PS_GET_HIGHEST_TAG_ID.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Loads tags, groups, and media into a menagerie from the database.
     * <p>
     * WARNING: Very expensive operation, should only be called once.
     *
     * @param menagerie Menagerie to load objects into.
     */
    public void loadIntoMenagerie(Menagerie menagerie) throws SQLException {
        loadTags(menagerie);
        Main.log.info("Finished loading " + menagerie.getTags().size() + " tags from database");
        loadTagNotes(menagerie);
        Main.log.info("Finished loading all tag notes into tags from database");
        loadGroups(menagerie);
        loadMedia(menagerie);
        sortGroupElements(menagerie);
        Main.log.info("Finished loading " + menagerie.getItems().size() + " items from database");
        loadTagsForItems(menagerie);
        Main.log.info("Finished loading tags for " + menagerie.getItems().size() + " items from database");
    }

    /**
     * Sort group elements so they're aligned with their page indices.
     *
     * @param menagerie Menagerie to sort groups in.
     */
    private void sortGroupElements(Menagerie menagerie) {
        for (Item item : menagerie.getItems()) {
            if (item instanceof GroupItem) {
                ((GroupItem) item).getElements().sort(Comparator.comparingInt(MediaItem::getPageIndex));
            }
        }
    }

    /**
     * Loads all tag notes into their tags.
     *
     * @param menagerie Menagerie to load tag notes into.
     * @throws SQLException When database query fails.
     */
    private void loadTagNotes(Menagerie menagerie) throws SQLException {
        synchronized (PS_GET_TAG_NOTES) {
            try (ResultSet rs = PS_GET_TAG_NOTES.executeQuery()) {
                while (rs.next()) {
                    Tag tag = menagerie.getTagByID(rs.getInt("tag_id"));

                    if (tag != null) {
                        tag.getNotes().add(rs.getNString("note"));
                    } else {
                        Main.log.severe(String.format("Tag with id %d does not exist, but exists in tag_notes", rs.getInt("tag_id")));
                    }
                }
            }
        }
    }

    /**
     * Loads all tags from the database.
     * <p>
     * WARNING: This call is expensive and should only be called once per Menagerie environment.
     *
     * @param menagerie Menagerie to load tags into.
     * @throws SQLException When database query fails.
     */
    private void loadTags(Menagerie menagerie) throws SQLException {
        synchronized (PS_GET_TAGS) {
            try (ResultSet rs = PS_GET_TAGS.executeQuery()) {
                while (rs.next()) {
                    menagerie.getTags().add(new Tag(menagerie, rs.getInt("id"), rs.getNString("name"), rs.getNString("color")));
                }
            }
        }
    }

    /**
     * Loads all groups from the database.
     * <p>
     * WARNING: This call is very expensive and should only be called once.
     *
     * @param menagerie Menagerie to load tags into.
     * @throws SQLException When database query fails.
     */
    private void loadGroups(Menagerie menagerie) throws SQLException {
        synchronized (PS_GET_GROUPS) {
            try (ResultSet rs = PS_GET_GROUPS.executeQuery()) {
                while (rs.next()) {
                    menagerie.getItems().add(new GroupItem(menagerie, rs.getInt("items.id"), rs.getLong("items.added"), rs.getNString("groups.title")));
                }
            }
        }
    }

    /**
     * Loads media items from the database.
     * <p>
     * WARNING: This call is very expensive and should only be called once.
     */
    private void loadMedia(Menagerie menagerie) throws SQLException {
        synchronized (PS_GET_MEDIA) {
            try (ResultSet rs = PS_GET_MEDIA.executeQuery()) {
                while (rs.next()) {
                    ImageHistogram histogram = null;
                    InputStream histAlpha = rs.getBinaryStream("media.hist_a");
                    if (histAlpha != null) {
                        try {
                            histogram = new ImageHistogram(histAlpha, rs.getBinaryStream("media.hist_r"), rs.getBinaryStream("media.hist_g"), rs.getBinaryStream("media.hist_b"));
                        } catch (HistogramReadException e) {
                            Main.log.log(Level.SEVERE, "Histogram failed to load from database", e);
                        }
                    }

                    // Try to get group
                    int gid = rs.getInt("media.gid");
                    GroupItem group = null;
                    if (gid != 0) {
                        for (Item item : menagerie.getItems()) {
                            if (item instanceof GroupItem && item.getId() == gid) {
                                group = (GroupItem) item;
                                break;
                            }
                        }
                    }

                    MediaItem media = new MediaItem(menagerie, rs.getInt("items.id"), rs.getLong("items.added"), rs.getInt("media.page"), group, new File(rs.getNString("media.path")), rs.getNString("media.md5"), histogram);
                    menagerie.getItems().add(media);
                    if (group != null) group.getElements().add(media);
                }
            }
        }
    }

    /**
     * Loads tags for items from the database.
     *
     * @param menagerie Menagerie environment to work in.
     * @throws SQLException If database query fails.
     */
    private void loadTagsForItems(Menagerie menagerie) throws SQLException {
        for (Item item : menagerie.getItems()) {
            synchronized (PS_GET_TAGS_FOR_ITEM) {
                PS_GET_TAGS_FOR_ITEM.setInt(1, item.getId());
                try (ResultSet rs = PS_GET_TAGS_FOR_ITEM.executeQuery()) {
                    while (rs.next()) {
                        Tag tag = menagerie.getTagByID(rs.getInt("tag_id"));
                        if (tag != null) {
                            tag.incrementFrequency();
                            item.getTags().add(tag);
                        } else {
                            Main.log.warning("Major issue, tag wasn't loaded in but somehow still exists in the database: " + rs.getInt("tag_id"));
                        }
                    }
                }
            }
        }
    }

    /**
     * Shuts down the database and runs a defrag operation to compress database file size.
     *
     * @throws SQLException If exception occurs.
     */
    public void shutdownDefrag() throws SQLException {
        synchronized (PS_SHUTDOWN_DEFRAG) {
            PS_SHUTDOWN_DEFRAG.executeUpdate();
        }
    }

    /**
     * Cleanly stops this thread by waiting for queued updates to complete. Does not block.
     */
    public void cleanStop() {
        running = false;
        loggingTimer.cancel();
    }

}
