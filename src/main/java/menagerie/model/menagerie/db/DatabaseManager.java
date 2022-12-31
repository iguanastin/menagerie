/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model.menagerie.db;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.util.listeners.ObjectListener;

/**
 * Menagerie database updater thread. Provides methods for synchronous database updates as well as asynchronous updates.
 */
public class DatabaseManager extends Thread {

  private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
  private static final String FINISHED_LOADING_LOG_TEMPLATE =
      "Finished loading %s %s from database";

  // Media
  private final PreparedStatement PS_GET_MEDIA;
  private final PreparedStatement PS_CREATE_MEDIA;
  private final PreparedStatement PS_SET_MEDIA_GID;
  private final PreparedStatement PS_SET_MEDIA_MD5;
  private final PreparedStatement PS_SET_MEDIA_PATH;
  private final PreparedStatement PS_SET_MEDIA_HISTOGRAM;
  private final PreparedStatement PS_SET_MEDIA_PAGE;
  private final PreparedStatement PS_SET_MEDIA_NOSIMILAR;
  // Non Duplicates
  private final PreparedStatement PS_ADD_NON_DUPE;
  private final PreparedStatement PS_REMOVE_NON_DUPE;
  private final PreparedStatement PS_GET_NON_DUPES;
  private final PreparedStatement PS_GET_NON_DUPES_COUNT;
  // Groups
  private final PreparedStatement PS_GET_GROUPS;
  private final PreparedStatement PS_CREATE_GROUP;
  private final PreparedStatement PS_SET_GROUP_TITLE;
  // Items
  private final PreparedStatement PS_GET_ITEM_COUNT;
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
  private final PreparedStatement PS_GET_TAGS_FOR_ITEM;
  private final PreparedStatement PS_GET_TAG_NOTES;
  private final PreparedStatement PS_GET_TAGS;
  private final PreparedStatement PS_GET_TAG_COUNT;
  // Counters
  private final PreparedStatement PS_GET_HIGHEST_ITEM_ID;
  private final PreparedStatement PS_GET_HIGHEST_TAG_ID;
  // Teardown
  private final PreparedStatement PS_SHUTDOWN_DEFRAG;

  private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  private volatile boolean running = false;

  private MenagerieDatabaseLoadListener loadListener = null;
  private ObjectListener<Integer> queueSizeListener = null;

  private final Timer loggingTimer = new Timer("Logging Timer", true);
  private final Lock loggingLock = new ReentrantLock();
  private int databaseUpdates = 0;
  private long lastLog = System.currentTimeMillis();


  public DatabaseManager(Connection database) throws SQLException {
    super("DatabaseManager Thread");

    // ------------------------------------ Init statements -----------------------------------
    // Media
    PS_GET_MEDIA = database.prepareStatement(
        "SELECT items.id, items.added, media.gid, media.page, media.no_similar, media.path, media.md5, media.hist_a, media.hist_r, media.hist_g, media.hist_b FROM media JOIN items ON items.id=media.id;");
    PS_CREATE_MEDIA = database.prepareStatement(
        "INSERT INTO media(id, path, md5, hist_a, hist_r, hist_g, hist_b) VALUES (?, ?, ?, ?, ?, ?, ?);");
    PS_SET_MEDIA_GID = database.prepareStatement("UPDATE media SET gid=? WHERE id=?;");
    PS_SET_MEDIA_MD5 = database.prepareStatement("UPDATE media SET md5=? WHERE id=?;");
    PS_SET_MEDIA_PATH = database.prepareStatement("UPDATE media SET path=? WHERE id=?;");
    PS_SET_MEDIA_HISTOGRAM = database.prepareStatement(
        "UPDATE media SET hist_a=?, hist_r=?, hist_g=?, hist_b=? WHERE id=?");
    PS_SET_MEDIA_PAGE = database.prepareStatement("UPDATE media SET page=? WHERE id=?;");
    PS_SET_MEDIA_NOSIMILAR = database.prepareStatement("UPDATE media SET no_similar=? WHERE id=?;");
    // Non Duplicates
    PS_GET_NON_DUPES = database.prepareStatement("SELECT item_1, item_2 FROM non_dupes;");
    PS_ADD_NON_DUPE =
        database.prepareStatement("INSERT INTO non_dupes(item_1, item_2) VALUES(?, ?);");
    PS_REMOVE_NON_DUPE = database.prepareStatement(
        "DELETE FROM non_dupes WHERE (item_1=? AND item_2=?) OR (item_2=? AND item_1=?);");
    PS_GET_NON_DUPES_COUNT = database.prepareStatement("SELECT count(*) FROM non_dupes;");
    // Groups
    PS_GET_GROUPS = database.prepareStatement(
        "SELECT items.id, items.added, groups.title FROM groups JOIN items ON items.id=groups.id;");
    PS_CREATE_GROUP = database.prepareStatement("INSERT INTO groups(id, title) VALUES (?, ?);");
    PS_SET_GROUP_TITLE = database.prepareStatement("UPDATE groups SET title=? WHERE id=?;");
    // Items
    PS_GET_ITEM_COUNT = database.prepareStatement("SELECT count(*) FROM items;");
    PS_DELETE_ITEM = database.prepareStatement("DELETE FROM items WHERE id=?;");
    PS_CREATE_ITEM = database.prepareStatement("INSERT INTO items(id, added) VALUES (?, ?);");
    // Tags
    PS_DELETE_TAG = database.prepareStatement("DELETE FROM tags WHERE id=?;");
    PS_CREATE_TAG = database.prepareStatement("INSERT INTO tags(id, name) VALUES (?, ?);");
    PS_ADD_TAG_TO_ITEM =
        database.prepareStatement("INSERT INTO tagged(item_id, tag_id) VALUES (?, ?);");
    PS_REMOVE_TAG_FROM_ITEM =
        database.prepareStatement("DELETE FROM tagged WHERE item_id=? AND tag_id=?;");
    PS_ADD_TAG_NOTE =
        database.prepareStatement("INSERT INTO tag_notes(tag_id, note) VALUES (?, ?);");
    PS_REMOVE_TAG_NOTE =
        database.prepareStatement("DELETE TOP 1 FROM tag_notes WHERE tag_id=? AND note LIKE ?;");
    PS_SET_TAG_COLOR = database.prepareStatement("UPDATE tags SET color=? WHERE id=?;");
    PS_GET_TAGS = database.prepareStatement("SELECT * FROM tags;");
    PS_GET_TAG_COUNT = database.prepareStatement("SELECT count(*) FROM tags;");
    PS_GET_TAGS_FOR_ITEM =
        database.prepareStatement("SELECT tagged.tag_id FROM tagged WHERE tagged.item_id=?;");
    PS_GET_TAG_NOTES = database.prepareStatement("SELECT * FROM tag_notes;");
    // Counters
    PS_GET_HIGHEST_ITEM_ID =
        database.prepareStatement("SELECT TOP 1 id FROM items ORDER BY id DESC;");
    PS_GET_HIGHEST_TAG_ID =
        database.prepareStatement("SELECT TOP 1 id FROM tags ORDER BY id DESC;");
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
        if (queueSizeListener != null) {
          queueSizeListener.pass(queue.size());
        }
        try {
          loggingLock.lock();
          databaseUpdates++;
        } finally {
          loggingLock.unlock();
        }

        try {
          job.run();
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Exception while running database updater job", e);
        }
      } catch (InterruptedException e) {
        LOGGER.log(Level.WARNING, "Database updater interrupted while waiting for queue", e);
      }
    }
  }

  /**
   * Initializes the logging timer that outputs update counts regularly.
   */
  private void startLoggingTimer() {
    final var task = new TimerTask() {
      @Override
      public void run() {
        try {
          loggingLock.lock();
          if (databaseUpdates > 0) {
            final var minutesSinceLastLog = (System.currentTimeMillis() - lastLog) / 1000.0 / 60.0;
            LOGGER.info(() -> String.format("DatabaseManager updated %d times in the last %.2fm",
                databaseUpdates, minutesSinceLastLog));
            lastLog = System.currentTimeMillis();
            databaseUpdates = 0;
          }
        } finally {
          loggingLock.unlock();
        }
      }
    };

    loggingTimer.schedule(task, 60000, 60000);
  }

  public void setLoadListener(MenagerieDatabaseLoadListener loadListener) {
    this.loadListener = loadListener;
  }

  public void setQueueSizeListener(ObjectListener<Integer> queueSizeListener) {
    this.queueSizeListener = queueSizeListener;
  }

  /**
   * Enqueues a job to this thread. FIFO.
   *
   * @param job Job to enqueue.
   */
  public void enqueue(Runnable job) {
    queue.add(job);
    if (queueSizeListener != null) {
      queueSizeListener.pass(queue.size());
    }
  }

  @FunctionalInterface
  private interface Action {
    void execute() throws SQLException;
  }

  private void executeAsync(Action action, Supplier<String> errorMessage) {
    queue.add(() -> {
      try {
        action.execute();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e, errorMessage);
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
  private void setMD5(int id, String md5) throws SQLException {
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
    executeAsync(() -> setMD5(id, md5), () -> "Failed to set md5 async: " + id + " - md5: " + md5);
  }

  /**
   * Stores a histogram in the database.
   *
   * @param id   ID of item to update.
   * @param hist Histogram to store.
   * @throws SQLException If database update fails.
   */
  private void setHist(int id, ImageHistogram hist) throws SQLException {
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
    executeAsync(() -> setHist(id, hist), () -> "Failed to set histogram async: " + id);
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
   * Tags an item with a tag.
   *
   * @param item ID of item.
   * @param tag  ID of tag.
   * @throws SQLException If database update fails.
   */
  private void tagItem(int item, int tag) throws SQLException {
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
    executeAsync(() -> tagItem(item, tag),
        () -> "Failed to tag item: " + item + " with tag: " + tag);
  }

  /**
   * Removes a tag from an item.
   *
   * @param item ID of item.
   * @param tag  ID of tag.
   * @throws SQLException If database update fails.
   */
  private void untagItem(int item, int tag) throws SQLException {
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
    executeAsync(() -> untagItem(item, tag),
        () -> "Failed to untag item: " + item + " from tag: " + tag);
  }

  /**
   * Removes an item from the database.
   *
   * @param id ID of item.
   * @throws SQLException If database update fails.
   */
  private void removeItem(int id) throws SQLException {
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
    executeAsync(() -> removeItem(id), () -> "Failed to remove item: " + id);
  }

  /**
   * Creates a tag in the database.
   *
   * @param id   ID of tag.
   * @param name Name of tag.
   * @throws SQLException If database update fails.
   */
  private void createTag(int id, String name) throws SQLException {
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
    executeAsync(() -> createTag(id, name),
        () -> "Failed to create tag: " + id + " - \"" + name + "\"");
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
   * Stores a new MediaItem in the database.
   *
   * @param media Item to store.
   * @throws SQLException If database update fails.
   */
  public void createMedia(MediaItem media) throws SQLException {
    createItem(media.getId(), media.getDateAdded());
    createMediaData(media);
  }

  private void createMediaData(MediaItem media) throws SQLException {
    synchronized (PS_CREATE_MEDIA) {
      PS_CREATE_MEDIA.setInt(1, media.getId());
      PS_CREATE_MEDIA.setNString(2, media.getFile().getAbsolutePath());
      PS_CREATE_MEDIA.setNString(3, media.getMD5());
      PS_CREATE_MEDIA.setBinaryStream(4, null);
      PS_CREATE_MEDIA.setBinaryStream(5, null);
      PS_CREATE_MEDIA.setBinaryStream(6, null);
      PS_CREATE_MEDIA.setBinaryStream(7, null);
      setMediaHistogramData(media);
      PS_CREATE_MEDIA.executeUpdate();
    }
  }

  private void setMediaHistogramData(MediaItem media) throws SQLException {
    if (media.getHistogram() != null) {
      PS_CREATE_MEDIA.setBinaryStream(4, media.getHistogram().getAlphaAsInputStream());
      PS_CREATE_MEDIA.setBinaryStream(5, media.getHistogram().getRedAsInputStream());
      PS_CREATE_MEDIA.setBinaryStream(6, media.getHistogram().getGreenAsInputStream());
      PS_CREATE_MEDIA.setBinaryStream(7, media.getHistogram().getBlueAsInputStream());
    }
  }

  /**
   * Stores a new GroupItem in the database.
   *
   * @param group Group to store.
   * @throws SQLException If database update fails.
   */
  public void createGroup(GroupItem group) throws SQLException {
    createItem(group.getId(), group.getDateAdded());
    createGroupData(group);
  }

  private void createGroupData(GroupItem group) throws SQLException {
    synchronized (PS_CREATE_GROUP) {
      PS_CREATE_GROUP.setInt(1, group.getId());
      PS_CREATE_GROUP.setNString(2, group.getTitle());
      PS_CREATE_GROUP.executeUpdate();
    }
  }

  private void createItem(int id, long dateAdded) throws SQLException {
    synchronized (PS_CREATE_ITEM) {
      PS_CREATE_ITEM.setInt(1, id);
      PS_CREATE_ITEM.setLong(2, dateAdded);
      PS_CREATE_ITEM.executeUpdate();
    }
  }

  /**
   * Sets the GID (parent group ID) of a media item.
   *
   * @param id  ID of media.
   * @param gid ID of group.
   * @throws SQLException When database update fails.
   */
  private void setMediaGID(int id, Integer gid) throws SQLException {
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
    executeAsync(() -> setMediaGID(id, gid),
        () -> String.format("Failed to set media GID async. ID: %d, GID: %d", id, gid));
  }

  /**
   * Sets the page index of a media item.
   *
   * @param id   ID of media item.
   * @param page Page index to set.
   * @throws SQLException If database update fails.
   */
  private void setMediaPage(int id, int page) throws SQLException {
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
    executeAsync(() -> setMediaPage(id, page),
        () -> String.format("Failed to set media page index. ID: %d, Page: %d", id, page));
  }

  /**
   * Sets the title of a group.
   *
   * @param id    ID of group.
   * @param title Title to set to.
   */
  private void setGroupTitle(int id, String title) throws SQLException {
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
    executeAsync(() -> setGroupTitle(id, title),
        () -> "Failed to set group title. ID: " + id + ", Title: " + title);
  }

  /**
   * Inserts a note into the tag_notes table.
   *
   * @param id   ID of tag note is attached to.
   * @param note The note.
   * @throws SQLException When database update fails.
   */
  private void addTagNote(int id, String note) throws SQLException {
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
    executeAsync(() -> addTagNote(id, note),
        () -> String.format("Failed to insert tag note. Tag ID: %d, Note: \"%s\"", id, note));
  }

  /**
   * Deletes a note from the tag_notes table.
   *
   * @param id   ID of tag note is attached to.
   * @param note The note.
   * @throws SQLException If database update fails.
   */
  private void removeTagNote(int id, String note) throws SQLException {
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
    executeAsync(() -> removeTagNote(id, note),
        () -> String.format("Failed to remove tag note. Tag ID: %d, Note: \"%s\"", id, note));
  }

  /**
   * Sets the color of a tag.
   *
   * @param id    ID of tag.
   * @param color Color to set.
   * @throws SQLException If database update fails.
   */
  private void setTagColor(int id, String color) throws SQLException {
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
    executeAsync(() -> setTagColor(id, color),
        () -> String.format("Failed to set tag color: ID: %d, Color: %s", id, color));
  }

  /**
   * Sets the flag of a media item signifying it has no similar items with the weakest confidence.
   *
   * @param id ID of media.
   * @param b  noSimilar value.
   * @throws SQLException When database update fails.
   */
  private void setMediaNoSimilar(int id, boolean b) throws SQLException {
    synchronized (PS_SET_MEDIA_NOSIMILAR) {
      PS_SET_MEDIA_NOSIMILAR.setBoolean(1, b);
      PS_SET_MEDIA_NOSIMILAR.setInt(2, id);
      PS_SET_MEDIA_NOSIMILAR.executeUpdate();
    }
  }

  /**
   * Queues a value to be set for the media no_similar flag.
   *
   * @param id ID of media.
   * @param b  Flag.
   */
  public void setMediaNoSimilarAsync(int id, boolean b) {
    executeAsync(() -> setMediaNoSimilar(id, b),
        () -> "Failed to set media no_similar. ID: " + id + ", no_similar: " + b);
  }

  private void addNonDuplicate(int id1, int id2) throws SQLException {
    synchronized (PS_ADD_NON_DUPE) {
      PS_ADD_NON_DUPE.setInt(1, id1);
      PS_ADD_NON_DUPE.setInt(2, id2);
      PS_ADD_NON_DUPE.executeUpdate();
    }
  }

  public void addNonDuplicateAsync(int id1, int id2) {
    executeAsync(() -> addNonDuplicate(id1, id2),
        () -> "Failed to add to non_dupes: " + id1 + ", " + id2);
  }

  private void removeNonDuplicate(int id1, int id2) throws SQLException {
    synchronized (PS_REMOVE_NON_DUPE) {
      PS_REMOVE_NON_DUPE.setInt(1, id1);
      PS_REMOVE_NON_DUPE.setInt(2, id2);
      PS_REMOVE_NON_DUPE.setInt(3, id1);
      PS_REMOVE_NON_DUPE.setInt(4, id2);
      PS_REMOVE_NON_DUPE.executeUpdate();
    }
  }

  public void removeNonDuplicateAsync(int id1, int id2) {
    executeAsync(() -> removeNonDuplicate(id1, id2),
        () -> "Failed to remove from non_dupes: " + id1 + ", " + id2);
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
    LOGGER.info(() -> FINISHED_LOADING_LOG_TEMPLATE
        .formatted(menagerie.getTags().size(), "tags"));

    loadTagNotes(menagerie);
    LOGGER.info(() -> FINISHED_LOADING_LOG_TEMPLATE
        .formatted("all", "tag notes into tags"));

    loadItems(menagerie);
    sortGroupElements(menagerie);
    LOGGER.info(() -> FINISHED_LOADING_LOG_TEMPLATE
        .formatted(menagerie.getItems().size(), "items"));

    loadTagsForItems(menagerie);
    LOGGER.info(() -> FINISHED_LOADING_LOG_TEMPLATE
        .formatted(menagerie.getItems().size(), "items"));

    loadNonDupes(menagerie);
    LOGGER.info(() -> FINISHED_LOADING_LOG_TEMPLATE
        .formatted(menagerie.getNonDuplicates().size(), "non-duplicates"));
  }

  /**
   * Sort group elements, so they're aligned with their page indices.
   *
   * @param menagerie Menagerie to sort groups in.
   */
  private void sortGroupElements(Menagerie menagerie) {
    for (Item item : menagerie.getItems()) {
      if (item instanceof GroupItem groupItem) {
        groupItem.getElements().sort(Comparator.comparingInt(MediaItem::getPageIndex));
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
          final var tagId = rs.getInt("tag_id");
          Tag tag = menagerie.getTagByID(tagId);

          if (tag != null) {
            tag.getNotes().add(rs.getNString("note"));
          } else {
            LOGGER.severe(() -> String.format(
                "Tag with id %d does not exist, but exists in tag_notes", tagId));
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
    int total = getTagCount();
    if (loadListener != null) {
      loadListener.startTagLoading(total);
    }
    getAllTags(menagerie, total);
  }

  private int getTagCount() throws SQLException {
    int total = 0;
    synchronized (PS_GET_TAG_COUNT) {
      try (ResultSet rs = PS_GET_TAG_NOTES.executeQuery()) {
        if (rs.next()) {
          total = rs.getInt(1);
        }
      }
    }
    return total;
  }

  private void getAllTags(Menagerie menagerie, int totalTagCount) throws SQLException {
    synchronized (PS_GET_TAGS) {
      try (ResultSet rs = PS_GET_TAGS.executeQuery()) {
        int i = 0;
        while (rs.next()) {
          i++;
          final var id = rs.getInt("id");
          final var name = rs.getNString("name");
          final var color = rs.getNString("color");
          menagerie.getTags().add(new Tag(menagerie, id, name, color));
          if (loadListener != null) {
            loadListener.tagsLoading(i, totalTagCount);
          }
        }
      }
    }
  }

  private void loadNonDupes(Menagerie menagerie) throws SQLException {
    if (loadListener != null) {
      loadListener.gettingNonDupeList();
    }

    int total = getDupesCount();
    if (loadListener != null) {
      loadListener.startNonDupeLoading(total);
    }

    getAllDupes(menagerie, total);
  }

  private int getDupesCount() throws SQLException {
    int total = 0;
    synchronized (PS_GET_NON_DUPES_COUNT) {
      try (ResultSet rs = PS_GET_NON_DUPES_COUNT.executeQuery()) {
        if (rs.next()) {
          total = rs.getInt(1);
        }
      }
    }
    return total;
  }

  private void getAllDupes(Menagerie menagerie, int totalDupesCount) throws SQLException {
    synchronized (PS_GET_NON_DUPES) {
      try (ResultSet rs = PS_GET_NON_DUPES.executeQuery()) {
        int i = 0;
        while (rs.next()) {
          i++;
          menagerie.getNonDuplicates().add(
              new SimilarPair<>((MediaItem) menagerie.getItemByID(rs.getInt(1)),
                  (MediaItem) menagerie.getItemByID(rs.getInt(2)), 0));
          if (loadListener != null) {
            loadListener.nonDupeLoading(i, totalDupesCount);
          }
        }
      }
    }
  }

  // REENG: this method is still too large

  /**
   * Loads all items from the database.
   * <p>
   * WARNING: This call is very expensive and should only be called once.
   *
   * @param menagerie Menagerie to load items into.
   * @throws SQLException When database query fails.
   */
  private void loadItems(Menagerie menagerie) throws SQLException {
    if (loadListener != null) {
      loadListener.gettingItemList();
    }

    int totalItemCount = getItemCount();
    int currentItemCount = getAllGroups(menagerie, totalItemCount);

    synchronized (PS_GET_MEDIA) {
      try (ResultSet rs = PS_GET_MEDIA.executeQuery()) {
        if (loadListener != null) {
          loadListener.startedItemLoading(totalItemCount);
        }
        while (rs.next()) {
          currentItemCount++;

          ImageHistogram histogram = null;
          InputStream histAlpha = rs.getBinaryStream("media.hist_a");
          if (histAlpha != null) {
            try {
              final var histRed = rs.getBinaryStream("media.hist_r");
              final var histGreen = rs.getBinaryStream("media.hist_g");
              final var histBlue = rs.getBinaryStream("media.hist_b");
              histogram = new ImageHistogram(histAlpha, histRed, histGreen, histBlue);
            } catch (HistogramReadException e) {
              LOGGER.log(Level.SEVERE, "Histogram failed to load from database", e);
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

          final var id = rs.getInt("items.id");
          final var added = rs.getLong("items.added");
          final var pageIndex = rs.getInt("media.page");
          final var noSimilar = rs.getBoolean("media.no_similar");
          final var file = new File(rs.getNString("media.path"));
          final var hash = rs.getNString("media.md5");
          final var media = new MediaItem(
              menagerie, id, added, pageIndex, noSimilar, group, file, hash, histogram);
          menagerie.getItems().add(media);
          if (group != null) {
            group.getElements().add(media);
          }

          if (loadListener != null) {
            loadListener.itemsLoading(currentItemCount, totalItemCount);
          }
        }
      }
    }
  }

  private int getItemCount() throws SQLException {
    int total = 0;
    synchronized (PS_GET_ITEM_COUNT) {
      try (ResultSet rs = PS_GET_ITEM_COUNT.executeQuery()) {
        if (rs.next()) {
          total = rs.getInt(1);
        }
      }
    }
    return total;
  }

  private int getAllGroups(Menagerie menagerie, int totalItemCount) throws SQLException {
    int i = 0;
    synchronized (PS_GET_GROUPS) {
      try (ResultSet rs = PS_GET_GROUPS.executeQuery()) {
        while (rs.next()) {
          i++;
          final var id = rs.getInt("items.id");
          final var added = rs.getLong("items.added");
          final var title = rs.getNString("groups.title");
          menagerie.getItems().add(new GroupItem(menagerie, id, added, title));
          if (loadListener != null) {
            loadListener.itemsLoading(i, totalItemCount);
          }
        }
      }
    }
    return i;
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
            final var id = rs.getInt("tag_id");
            Tag tag = menagerie.getTagByID(id);
            if (tag != null) {
              tag.incrementFrequency();
              item.getTags().add(tag);
            } else {
              LOGGER.warning(() ->
                  "Major issue, tag wasn't loaded in but somehow still exists in the database: " +
                  id);
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

}
