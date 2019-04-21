package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
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

    public static final double MIN_CONFIDENCE = 0.9;
    public static final double MAX_CONFIDENCE = 1.0;

    // -------------------------------- Variables ------------------------------------

    private File file;
    private String md5;
    private ImageHistogram histogram;

    private SoftReference<Thumbnail> thumbnail;
    private WeakReference<Image> image;

    private GroupItem group;
    private int pageIndex;
    private boolean noSimilar;


    /**
     * @param menagerie    Menagerie this item belongs to.
     * @param id           Unique ID of this item.
     * @param dateAdded    Date this item was added.
     * @param pageIndex    Index of this item within its parent group.
     * @param hasNoSimilar This item has no similar items with the weakest confidence.
     * @param group        Parent group containing this item.
     * @param file         File this item points to.
     * @param md5          MD5 hash of the file.
     * @param histogram    Color histogram of the image. (If the media is an image)
     */
    public MediaItem(Menagerie menagerie, int id, long dateAdded, int pageIndex, boolean hasNoSimilar, GroupItem group, File file, String md5, ImageHistogram histogram) {
        super(menagerie, id, dateAdded);
        this.file = file;
        this.md5 = md5;
        this.histogram = histogram;
        this.group = group;
        this.pageIndex = pageIndex;
        this.noSimilar = hasNoSimilar;
    }

    /**
     * Constructs a bare bones media item.
     *
     * @param menagerie Menagerie this item belongs to.
     * @param id        Unique ID of this item.
     * @param dateAdded Date this item was added.
     * @param file      File this item points to.
     */
    public MediaItem(Menagerie menagerie, int id, long dateAdded, File file) {
        this(menagerie, id, dateAdded, 0, false, null, file, null, null);
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
        if (thumb == null && hasDatabase()) {
            try {
                thumb = menagerie.getDatabaseManager().getThumbnail(getId());
                if (thumb != null) thumbnail = new SoftReference<>(thumb);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Failed to get thumbnail from database: " + getId(), e);
            }
        }
        if (thumb == null && file != null) {
            try {
                thumb = new Thumbnail(file);
            } catch (IOException ignore) {
            }

            thumbnail = new SoftReference<>(thumb);

            if (thumb != null && hasDatabase()) {
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
     * Computes the MD5 of the file. No operation if MD5 already exists.
     */
    public void initializeMD5() {
        if (md5 != null) return;

        try {
            md5 = HexBin.encode(MD5Hasher.hash(getFile()));
            if (hasDatabase()) menagerie.getDatabaseManager().setMD5Async(getId(), md5);
        } catch (IOException e) {
            Main.log.log(Level.SEVERE, "Failed to hash file: " + getFile(), e);
        }
    }

    /**
     * @return The color histogram of the image. Null if this file is not an image.
     */
    public ImageHistogram getHistogram() {
        return histogram;
    }

    /**
     * Computes the color histogram of the image. No operation if file is not an image, or is a GIF image.
     */
    public void initializeHistogram() {
        if (!getFile().getName().toLowerCase().endsWith(".gif") && Filters.IMAGE_NAME_FILTER.accept(getFile())) {
            try {
                histogram = new ImageHistogram(getImageSynchronously());
                if (hasDatabase()) menagerie.getDatabaseManager().setHistAsync(getId(), histogram);
            } catch (HistogramReadException e) {
                Main.log.log(Level.WARNING, "Failed to create histogram for: " + getId(), e);
            }
        }
    }

    /**
     * @return The parent group of this item. Null if none.
     */
    public GroupItem getGroup() {
        return group;
    }

    /**
     * @return The index this item is in within the parent group.
     */
    public int getPageIndex() {
        return pageIndex;
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
     * @return True if this item has no similar items with the weakest confidence.
     */
    public boolean hasNoSimilar() {
        return noSimilar;
    }

    /**
     * @return True if this item has a parent group.
     */
    public boolean isInGroup() {
        return group != null;
    }

    /**
     * Renames a file to a new location on the local disk. Safe operation to maintain the file without losing track of it.
     *
     * @param dest Destination file to rename this file to.
     * @return True if successful.
     */
    public boolean moveFile(File dest) {
        if (getFile() == null || dest == null) return false;
        if (file.equals(dest)) return true;

        boolean succeeded = file.renameTo(dest);

        if (succeeded) {
            file = dest;

            if (hasDatabase()) {
                try {
                    menagerie.getDatabaseManager().setPath(getId(), file.getAbsolutePath());
                } catch (SQLException e) {
                    Main.log.log(Level.SEVERE, "Failed to update new path to file", e);
                }
            }
        }

        return succeeded;
    }

    /**
     * @param other Target to compare with.
     * @return Similarity to another image. 1 if MD5 hashes match, [0.0-1.0] if histograms exist, 0 otherwise.
     */
    public double getSimilarityTo(MediaItem other) {
        if (md5 != null && md5.equals(other.getMD5())) {
            return 1.0;
        } else if (histogram != null && other.getHistogram() != null) {
            return histogram.getSimilarity(other.getHistogram());
        }

        return 0;
    }

    /**
     * @param group The new parent group of this item.
     */
    void setGroup(GroupItem group) {
        this.group = group;

        Integer gid = null;
        if (group != null) gid = group.getId();

        if (hasDatabase()) menagerie.getDatabaseManager().setMediaGIDAsync(getId(), gid);
    }

    /**
     * Sets the index of this item.
     * <p>
     * This method does not change ordering in the parent group, and should only be used by the group as a utility.
     *
     * @param pageIndex Index to set to.
     */
    void setPageIndex(int pageIndex) {
        if (this.pageIndex == pageIndex) return;

        this.pageIndex = pageIndex;

        if (hasDatabase()) menagerie.getDatabaseManager().setMediaPageAsync(getId(), pageIndex);
    }

    /**
     * Sets the flag for this item signifying that there are no items in the database that are similar to this with the weakest confidence.
     *
     * @param b Has no similar items.
     */
    public void setHasNoSimilar(boolean b) {
        if (noSimilar != b && hasDatabase()) getDatabase().setMediaNoSimilarAsync(getId(), b);

        noSimilar = b;
    }

    /**
     * Forgets this item and deletes its file.
     *
     * @return True if successfully forgotten and deleted
     */
    @Override
    protected boolean delete() {
        if (!forget()) return false;

        FileUtils fu = FileUtils.getInstance();
        if (fu.hasTrash()) {
            try {
                fu.moveToTrash(new File[]{getFile()});
                return true;
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Unable to send file to recycle bin: " + getFile(), e);
                return false;
            }
        } else {
            return getFile().delete();
        }
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile() + "\"";
    }

}
