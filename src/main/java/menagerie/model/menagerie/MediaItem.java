package menagerie.model.menagerie;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.scene.image.Image;
import menagerie.gui.Main;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.util.Filters;
import menagerie.util.MD5Hasher;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

/**
 * A Menagerie item representing a media file of some form. Image and video.
 */
public class MediaItem extends Item {

    // -------------------------------- Variables ------------------------------------

    private File file;
    private String md5;
    private ImageHistogram histogram;

    private SoftReference<Thumbnail> thumbnail;
    private WeakReference<Image> image;

    private GroupItem group;


    /**
     * @param menagerie The Menagerie this item belongs to.
     * @param id        The unique ID of this item.
     * @param dateAdded The date this item was added to the Menagerie.
     * @param file      The file this item points to.
     * @param md5       The MD5 hash of the file.
     * @param histogram The color histogram of the image. (If the media is an image)
     */
    public MediaItem(Menagerie menagerie, int id, long dateAdded, File file, String md5, ImageHistogram histogram) {
        super(menagerie, id, dateAdded);
        this.file = file;
        this.md5 = md5;
        this.histogram = histogram;
    }

    /**
     * @return The file of this item.
     */
    public File getFile() {
        return file;
    }

    /**
     * Creates a thumbnail if one does not already exist.
     *
     * @return The thumbnail of this media.
     */
    @Override
    public Thumbnail getThumbnail() {
        Thumbnail thumb = null;
        if (thumbnail != null) thumb = thumbnail.get();
        if (thumb == null) {
            try {
                thumb = menagerie.getDatabaseManager().getThumbnail(getId());
                if (thumb != null) thumbnail = new SoftReference<>(thumb);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to get thumbnail from database: " + getId(), e);
            }
        }
        if (thumb == null) {
            try {
                thumb = new Thumbnail(file);
            } catch (IOException ignore) {
            }

            thumbnail = new SoftReference<>(thumb);

            if (thumb != null) {
                if (thumb.isLoaded()) {
                    menagerie.getDatabaseManager().setThumbnailAsync(getId(), thumb.getImage());
                } else {
                    thumb.addImageLoadedListener(image1 -> menagerie.getDatabaseManager().setThumbnailAsync(getId(), image1));
                }
            }
        }

        return thumb;
    }

    /**
     * @return The full size image, if this item represents an image.
     */
    public Image getImage() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString(), true);
            image = new WeakReference<>(img);
        }
        return img;
    }

    /**
     * @return The full size image, guaranteed to be loaded.
     */
    private Image getImageSynchronously() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString());
            image = new WeakReference<>(img);
        } else if (img.isBackgroundLoading() && img.getProgress() != 1 && !img.isError()) {
            CountDownLatch latch = new CountDownLatch(1);
            Image finalImg = img;
            img.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (finalImg.isError() || newValue.equals(1)) {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            if (img.isError()) {
                img = new Image(file.toURI().toString());
            } else {
                return img;
            }
        }
        return img;
    }

    /**
     * @return The MD5 hash string of the file.
     */
    public String getMD5() {
        return md5;
    }

    /**
     * @return The color histogram of the image. Null if this file is not an image.
     */
    public ImageHistogram getHistogram() {
        return histogram;
    }

    /**
     * @return True if the file is accepted by the image file filter.
     * @see Filters
     */
    public boolean isImage() {
        return Filters.IMAGE_NAME_FILTER.accept(file);
    }

    /**
     * @return True if the file is accepted by the video file filter.
     * @see Filters
     */
    public boolean isVideo() {
        return Filters.VIDEO_NAME_FILTER.accept(file);
    }

    /**
     * Computes the MD5 of the file. No operation if MD5 already exists.
     */
    public void initializeMD5() {
        if (md5 != null) return;

        try {
            md5 = HexBin.encode(MD5Hasher.hash(getFile()));
            menagerie.getDatabaseManager().setMD5Async(getId(), md5);
        } catch (IOException e) {
            Main.log.log(Level.SEVERE, "Failed to hash file: " + getFile(), e);
        }
    }

    /**
     * Computes the color histogram of the image. No operation if file is not an image, or is a GIF image.
     */
    public void initializeHistogram() {
        if (!getFile().getName().toLowerCase().endsWith(".gif") && Filters.IMAGE_NAME_FILTER.accept(getFile())) {
            try {
                histogram = new ImageHistogram(getImageSynchronously());
                menagerie.getDatabaseManager().setHistAsync(getId(), histogram);
            } catch (HistogramReadException e) {
                Main.log.log(Level.WARNING, "Failed to create histogram for: " + getId(), e);
            }
        }
    }

    /**
     * Renames a file to a new location on the local disk. Safe operation to maintain the file without losing track of it.
     *
     * @param dest Destination file to rename this file to.
     * @return True if successful.
     */
    public boolean renameTo(File dest) {
        if (file.equals(dest)) return true;

        boolean succeeded = file.renameTo(dest);

        if (succeeded) {
            file = dest;

            try {
                menagerie.getDatabaseManager().setPath(getId(), file.getAbsolutePath());
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to update new path to file", e);
            }
        }

        return succeeded;
    }

    /**
     * @param other                     Target to compare with.
     * @param compareBlackAndWhiteHists Compare black and white images. Poor accuracy.
     * @return Similarity to another image. 1 if MD5 hashes match, [0.0-1.0] if histograms exist, 0 otherwise.
     */
    public double getSimilarityTo(MediaItem other, boolean compareBlackAndWhiteHists) {
        if (md5 != null && md5.equals(other.getMD5())) {
            return 1.0;
        } else if (histogram != null && other.getHistogram() != null) {
            if (compareBlackAndWhiteHists || (histogram.isColorful() && other.getHistogram().isColorful())) {
                return histogram.getSimilarity(other.getHistogram());
            }
        }

        return 0;
    }

    /**
     * @param group The new parent group of this item.
     */
    void setGroup(GroupItem group) {
        this.group = group;
        //TODO update database
    }

    /**
     * @return True if this item has a parent group.
     */
    public boolean inGroup() {
        return group != null;
    }

    /**
     * @return The parent group of this item. Null if none.
     */
    public GroupItem getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\"";
    }

}
