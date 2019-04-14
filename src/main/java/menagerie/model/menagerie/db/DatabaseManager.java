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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private final PreparedStatement PS_DELETE_IMG;
    private final PreparedStatement PS_CREATE_IMG;
    private final PreparedStatement PS_DELETE_TAG;
    private final PreparedStatement PS_CREATE_TAG;
    private final PreparedStatement PS_SET_IMG_MD5;
    private final PreparedStatement PS_SET_IMG_HISTOGRAM;
    private final PreparedStatement PS_SET_IMG_THUMBNAIL;
    private final PreparedStatement PS_GET_IMG_THUMBNAIL;
    private final PreparedStatement PS_ADD_TAG_TO_IMG;
    private final PreparedStatement PS_REMOVE_TAG_FROM_IMG;
    private final PreparedStatement PS_SET_IMG_PATH;
    private final PreparedStatement PS_GET_HIGHEST_ITEM_ID;
    private final PreparedStatement PS_GET_HIGHEST_TAG_ID;
    private final PreparedStatement PS_GET_TAGS;
    private final PreparedStatement PS_GET_MEDIA;
    private final PreparedStatement PS_GET_GROUPS;
    private final PreparedStatement PS_GET_TAGS_FOR_ITEM;
    private final PreparedStatement PS_SHUTDOWN_DEFRAG;

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private final Timer loggingTimer = new Timer(true);
    private final Lock loggingLock = new ReentrantLock();
    private int databaseUpdates = 0;
    private long lastLog = System.currentTimeMillis();


    public DatabaseManager(Connection database) throws SQLException {

        // ------------------------------------ Init statements -----------------------------------
        PS_SET_IMG_MD5 = database.prepareStatement("UPDATE imgs SET imgs.md5=? WHERE imgs.id=?;");
        PS_SET_IMG_HISTOGRAM = database.prepareStatement("UPDATE imgs SET imgs.hist_a=?, imgs.hist_r=?, imgs.hist_g=?, imgs.hist_b=? WHERE imgs.id=?");
        PS_SET_IMG_THUMBNAIL = database.prepareStatement("UPDATE imgs SET imgs.thumbnail=? WHERE imgs.id=?;");
        PS_GET_IMG_THUMBNAIL = database.prepareStatement("SELECT imgs.thumbnail FROM imgs WHERE imgs.id=?;");
        PS_ADD_TAG_TO_IMG = database.prepareStatement("INSERT INTO tagged(img_id, tag_id) VALUES (?, ?);");
        PS_REMOVE_TAG_FROM_IMG = database.prepareStatement("DELETE FROM tagged WHERE img_id=? AND tag_id=?;");
        PS_DELETE_IMG = database.prepareStatement("DELETE FROM imgs WHERE imgs.id=?;");
        PS_CREATE_IMG = database.prepareStatement("INSERT INTO imgs(id, path, added, md5, hist_a, hist_r, hist_g, hist_b) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        PS_DELETE_TAG = database.prepareStatement("DELETE FROM tags WHERE tags.id=?;");
        PS_CREATE_TAG = database.prepareStatement("INSERT INTO tags(id, name) VALUES (?, ?);");
        PS_SET_IMG_PATH = database.prepareStatement("UPDATE imgs SET path=? WHERE id=?;");
        PS_GET_HIGHEST_ITEM_ID = database.prepareStatement("SELECT TOP 1 id FROM imgs ORDER BY id DESC;");
        PS_GET_HIGHEST_TAG_ID = database.prepareStatement("SELECT TOP 1 id FROM tags ORDER BY id DESC;");
        PS_GET_TAGS = database.prepareStatement("SELECT * FROM tags;");
        PS_GET_MEDIA = database.prepareStatement("SELECT * FROM imgs;"); // TODO
        //        PS_GET_GROUPS = database.prepareStatement("SELECT items.id, items.added, groups.title, FROM groups JOIN items ON items.id=groups.id;");
        PS_GET_GROUPS = database.prepareStatement("SELECT * FROM tags;"); // TODO remove this
        PS_GET_TAGS_FOR_ITEM = database.prepareStatement("SELECT tagged.tag_id FROM tagged WHERE tagged.img_id=?;"); // TODO
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
        synchronized (PS_SET_IMG_THUMBNAIL) {
            PS_SET_IMG_THUMBNAIL.setBinaryStream(1, ImageInputStreamConverter.imageToInputStream(thumbnail));
            PS_SET_IMG_THUMBNAIL.setInt(2, id);
            PS_SET_IMG_THUMBNAIL.executeUpdate();
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
        synchronized (PS_SET_IMG_MD5) {
            PS_SET_IMG_MD5.setNString(1, md5);
            PS_SET_IMG_MD5.setInt(2, id);
            PS_SET_IMG_MD5.executeUpdate();
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
        synchronized (PS_SET_IMG_HISTOGRAM) {
            PS_SET_IMG_HISTOGRAM.setBinaryStream(1, hist.getAlphaAsInputStream());
            PS_SET_IMG_HISTOGRAM.setBinaryStream(2, hist.getRedAsInputStream());
            PS_SET_IMG_HISTOGRAM.setBinaryStream(3, hist.getGreenAsInputStream());
            PS_SET_IMG_HISTOGRAM.setBinaryStream(4, hist.getBlueAsInputStream());
            PS_SET_IMG_HISTOGRAM.setInt(5, id);
            PS_SET_IMG_HISTOGRAM.executeUpdate();
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
        synchronized (PS_SET_IMG_PATH) {
            PS_SET_IMG_PATH.setNString(1, path);
            PS_SET_IMG_PATH.setInt(2, id);
            PS_SET_IMG_PATH.executeUpdate();
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
        synchronized (PS_ADD_TAG_TO_IMG) {
            PS_ADD_TAG_TO_IMG.setInt(1, item);
            PS_ADD_TAG_TO_IMG.setInt(2, tag);
            PS_ADD_TAG_TO_IMG.executeUpdate();
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
        synchronized (PS_REMOVE_TAG_FROM_IMG) {
            PS_REMOVE_TAG_FROM_IMG.setInt(1, item);
            PS_REMOVE_TAG_FROM_IMG.setInt(2, tag);
            PS_REMOVE_TAG_FROM_IMG.executeUpdate();
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
        synchronized (PS_DELETE_IMG) {
            PS_DELETE_IMG.setInt(1, id);
            PS_DELETE_IMG.executeUpdate();
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
        synchronized (PS_CREATE_IMG) {
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
     * Gets a thumbnail from the database.
     *
     * @param id ID of item.
     * @return Thumbnail of the item, or null if there is no thumbnail.
     * @throws SQLException If database query fails.
     */
    public Thumbnail getThumbnail(int id) throws SQLException {
        synchronized (PS_GET_IMG_THUMBNAIL) {
            PS_GET_IMG_THUMBNAIL.setInt(1, id);
            try (ResultSet rs = PS_GET_IMG_THUMBNAIL.executeQuery()) {
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
        //        loadGroups(menagerie);
        // TODO load groups
        loadMedia(menagerie);
        Main.log.info("Finished loading " + menagerie.getItems().size() + " items from database");
        loadTagsForItems(menagerie);
        Main.log.info("Finished loading tags for " + menagerie.getItems().size() + " items from database");
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
                    menagerie.getTags().add(new Tag(rs.getInt("id"), rs.getNString("name")));
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
                    InputStream histAlpha = rs.getBinaryStream("hist_a");
                    if (histAlpha != null) {
                        try {
                            histogram = new ImageHistogram(histAlpha, rs.getBinaryStream("hist_r"), rs.getBinaryStream("hist_g"), rs.getBinaryStream("hist_b"));
                        } catch (HistogramReadException e) {
                            Main.log.log(Level.SEVERE, "Histogram failed to load from database", e);
                        }
                    }

                    menagerie.getItems().add(new MediaItem(menagerie, rs.getInt("imgs.id"), rs.getLong("imgs.added"), new File(rs.getNString("imgs.path")), rs.getNString("md5"), histogram));
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
