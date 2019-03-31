package menagerie.model.menagerie.db;

import javafx.scene.image.Image;
import menagerie.gui.Main;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.util.ImageInputStreamConverter;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class DatabaseUpdater extends Thread {

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

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;


    public DatabaseUpdater(Connection database) throws SQLException {


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
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                Runnable job = queue.take();

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

    public void enqueue(Runnable job) {
        queue.add(job);
    }

    public void setThumbnail(int id, Image thumbnail) throws SQLException, IOException {
        synchronized (PS_SET_IMG_THUMBNAIL) {
            PS_SET_IMG_THUMBNAIL.setBinaryStream(1, ImageInputStreamConverter.imageToInputStream(thumbnail));
            PS_SET_IMG_THUMBNAIL.setInt(2, id);
            PS_SET_IMG_THUMBNAIL.executeUpdate();
        }
    }

    public void setThumbnailAsync(int id, Image thumbnail) {
        queue.add(() -> {
            try {
                setThumbnail(id, thumbnail);
            } catch (SQLException | IOException e) {
                Main.log.log(Level.SEVERE, "Failed to set thumbnail async: " + id, e);
            }
        });
    }

    public void setMD5(int id, String md5) throws SQLException {
        synchronized (PS_SET_IMG_MD5) {
            PS_SET_IMG_MD5.setNString(1, md5);
            PS_SET_IMG_MD5.setInt(2, id);
            PS_SET_IMG_MD5.executeUpdate();
        }
    }

    public void setMD5Async(int id, String md5) {
        queue.add(() -> {
            try {
                setMD5(id, md5);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set md5 async: " + id + " - md5: " + md5, e);
            }
        });
    }

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

    public void setHistAsync(int id, ImageHistogram hist) {
        queue.add(() -> {
            try {
                setHist(id, hist);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set histogram async: " + id, e);
            }
        });
    }

    public void setPath(int id, String path) throws SQLException {
        synchronized (PS_SET_IMG_PATH) {
            PS_SET_IMG_PATH.setNString(1, path);
            PS_SET_IMG_PATH.setInt(2, id);
            PS_SET_IMG_PATH.executeUpdate();
        }
    }

    public void setPathAsync(int id, String path) {
        queue.add(() -> {
            try {
                setPath(id, path);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to set new path: " + id + " - \"" + path + "\"", e);
            }
        });
    }

    public void tagItem(int item, int tag) throws SQLException {
        synchronized (PS_ADD_TAG_TO_IMG) {
            PS_ADD_TAG_TO_IMG.setInt(1, item);
            PS_ADD_TAG_TO_IMG.setInt(2, tag);
            PS_ADD_TAG_TO_IMG.executeUpdate();
        }
    }

    public void tagItemAsync(int item, int tag) {
        queue.add(() -> {
            try {
                tagItem(item, tag);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to tag item: " + item + " with tag: " + tag, e);
            }
        });
    }

    public void untagItem(int item, int tag) throws SQLException {
        synchronized (PS_REMOVE_TAG_FROM_IMG) {
            PS_REMOVE_TAG_FROM_IMG.setInt(1, item);
            PS_REMOVE_TAG_FROM_IMG.setInt(2, tag);
            PS_REMOVE_TAG_FROM_IMG.executeUpdate();
        }
    }

    public void untagItemAsync(int item, int tag) {
        queue.add(() -> {
            try {
                untagItem(item, tag);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to untag item: " + item + " from tag: " + tag, e);
            }
        });
    }

    public void removeItem(int id) throws SQLException {
        synchronized (PS_DELETE_IMG) {
            PS_DELETE_IMG.setInt(1, id);
            PS_DELETE_IMG.executeUpdate();
        }
    }

    public void removeItemAsync(int id) {
        queue.add(() -> {
            try {
                removeItem(id);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to remove item: " + id, e);
            }
        });
    }

    public void createTag(int id, String name) throws SQLException {
        synchronized (PS_CREATE_TAG) {
            PS_CREATE_TAG.setInt(1, id);
            PS_CREATE_TAG.setNString(2, name);
            PS_CREATE_TAG.executeUpdate();
        }
    }

    public void createTagAsync(int id, String name) {
        queue.add(() -> {
            try {
                createTag(id, name);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to create tag: " + id + " - \"" + name + "\"", e);
            }
        });
    }

    public void deleteTag(int id) throws SQLException {
        synchronized (PS_DELETE_TAG) {
            PS_DELETE_TAG.setInt(1, id);
            PS_DELETE_TAG.executeUpdate();
        }
    }

    public void deleteTagAsync(int id) {
        queue.add(() -> {
            try {
                deleteTag(id);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to delete tag: " + id, e);
            }
        });
    }

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

    public void createMediaAsync(MediaItem media) {
        queue.add(() -> {
            try {
                createMedia(media);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to create media async: " + media, e);
            }
        });
    }

    public Thumbnail getThumbnail(int id) throws SQLException {
        synchronized (PS_GET_IMG_THUMBNAIL) {
            PS_GET_IMG_THUMBNAIL.setInt(1, id);
            ResultSet rs = PS_GET_IMG_THUMBNAIL.executeQuery();
            if (rs.next()) {
                InputStream binaryStream = rs.getBinaryStream("thumbnail");
                if (binaryStream != null) {
                    return new Thumbnail(ImageInputStreamConverter.imageFromInputStream(binaryStream));
                }
            }
        }

        return null;
    }

    public void cleanStop() {
        running = false;
    }

}
