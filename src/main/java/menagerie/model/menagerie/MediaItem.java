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

package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
import org.apache.commons.codec.binary.Hex;
import javafx.beans.property.*;
import javafx.scene.image.Image;
import menagerie.gui.Thumbnail;
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
import java.util.logging.Logger;

/**
 * A Menagerie item representing a media file of some form. Image and video.
 */
// REENG: probably good idea to split into separate responsibilities (image and video)
public class MediaItem extends Item {

    private static final Logger LOGGER = Logger.getLogger(MediaItem.class.getName());

    public static final double MIN_CONFIDENCE = 0.9;
    public static final double MAX_CONFIDENCE = 1.0;

    // -------------------------------- Variables ------------------------------------

    private final ObjectProperty<File> file = new SimpleObjectProperty<>();
    private final StringProperty md5 = new SimpleStringProperty();
    private final ObjectProperty<ImageHistogram> histogram = new SimpleObjectProperty<>();

    private SoftReference<Thumbnail> thumbnail;
    private WeakReference<Image> image;

    private final ObjectProperty<GroupItem> group = new SimpleObjectProperty<>();
    private final IntegerProperty pageIndex = new SimpleIntegerProperty();
    private final BooleanProperty noSimilar = new SimpleBooleanProperty();


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
        this.file.set(file);
        this.md5.set(md5);
        this.histogram.set(histogram);
        this.group.set(group);
        this.pageIndex.set(pageIndex);
        this.noSimilar.set(hasNoSimilar);
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
        return file.get();
    }

    public ObjectProperty<File> fileProperty() {
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
        if (thumb == null && file.get() != null) {
            thumb = new Thumbnail(this, file.get());
            thumbnail = new SoftReference<>(thumb);
        }

        return thumb;
    }

    @Override
    public void purgeThumbnail() {
        thumbnail = null;
    }

    public void purgeImage() {
        image = null;
    }

    /**
     * @return The full size image, if this item represents an image.
     */
    public Image getImage() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.get().toURI().toString(), true);
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
            img = new Image(file.get().toURI().toString());
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
                img = new Image(file.get().toURI().toString());
            } else {
                return img;
            }
        }
        return img;
    }

    /**
     * @return The MD5 hash string of the file.
     */
    // REENG: extract hashing functionality via interface (allows to easily switch hashing algo)
    public String getMD5() {
        return md5.get();
    }

    public StringProperty md5Property() {
        return md5;
    }

    /**
     * Computes the MD5 of the file. No operation if MD5 already exists.
     */
    public void initializeMD5() {
        try {
            byte[] md5bytes = MD5Hasher.hash(getFile());
            md5.set(md5bytes != null ? Hex.encodeHexString(md5bytes) : null);
            if (hasDatabase()) menagerie.getDatabaseManager().setMD5Async(getId(), md5.get());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to hash file: " + getFile(), e);
        }
    }

    /**
     * @return The color histogram of the image. Null if this file is not an image.
     */
    public ImageHistogram getHistogram() {
        return histogram.get();
    }

    public ObjectProperty<ImageHistogram> histogramProperty() {
        return histogram;
    }

    /**
     * Computes the color histogram of the image. No operation if file is not an image, or is a GIF image.
     */
    public boolean initializeHistogram() {
        if (!getFile().getName().toLowerCase().endsWith(".gif") && Filters.IMAGE_NAME_FILTER.accept(getFile())) {
            try {
                histogram.set(new ImageHistogram(getImageSynchronously()));
                if (hasDatabase()) menagerie.getDatabaseManager().setHistAsync(getId(), histogram.get());
                return true;
            } catch (HistogramReadException e) {
                LOGGER.log(Level.WARNING, "Failed to create histogram for: " + getId(), e);
            }
        }

        return false;
    }

    /**
     * @return The parent group of this item. Null if none.
     */
    public GroupItem getGroup() {
        return group.get();
    }

    public ObjectProperty<GroupItem> groupProperty() {
        return group;
    }

    /**
     * @return The index this item is in within the parent group.
     */
    public int getPageIndex() {
        return pageIndex.get();
    }

    public IntegerProperty pageIndexProperty() {
        return pageIndex;
    }

    /**
     * @return True if the file is accepted by the image file filter.
     * @see Filters
     */
    public boolean isImage() {
        return Filters.IMAGE_NAME_FILTER.accept(file.get());
    }

    /**
     * @return True if the file is accepted by the video file filter.
     * @see Filters
     */
    public boolean isVideo() {
        return Filters.VIDEO_NAME_FILTER.accept(file.get());
    }

    /**
     * @return True if this item has no similar items with the weakest confidence.
     */
    public boolean hasNoSimilar() {
        return noSimilar.get();
    }

    public BooleanProperty noSimilarProperty() {
        return noSimilar;
    }

    /**
     * @return True if this item has a parent group.
     */
    public boolean isInGroup() {
        return group.get() != null;
    }

    /**
     * Renames a file to a new location on the local disk. Safe operation to maintain the file without losing track of it.
     *
     * @param dest Destination file to rename this file to.
     * @return True if successful.
     */
    public boolean moveFile(File dest) {
        if (getFile() == null || dest == null) return false;
        if (file.get().equals(dest)) return true;

        LOGGER.info("Moving file: " + getFile() + "\nTo: " + dest);

        boolean succeeded = file.get().renameTo(dest);

        if (succeeded) {
            file.set(dest);

            if (hasDatabase()) {
                try {
                    menagerie.getDatabaseManager().setPath(getId(), file.get().getAbsolutePath());
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to update new path to file", e);
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
        if (md5.get() != null && md5.get().equals(other.getMD5())) {
            return 1.0;
        } else if (histogram.get() != null && other.getHistogram() != null) {
            return histogram.get().getSimilarity(other.getHistogram());
        }

        return 0;
    }

    /**
     * @param group The new parent group of this item.
     */
    void setGroup(GroupItem group) {
        this.group.set(group);

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
        if (this.pageIndex.get() == pageIndex) return;

        this.pageIndex.set(pageIndex);

        if (hasDatabase()) menagerie.getDatabaseManager().setMediaPageAsync(getId(), pageIndex);
    }

    /**
     * Sets the flag for this item signifying that there are no items in the database that are similar to this with the weakest confidence.
     *
     * @param b Has no similar items.
     */
    public void setHasNoSimilar(boolean b) {
        if (noSimilar.get() != b && hasDatabase()) getDatabase().setMediaNoSimilarAsync(getId(), b);

        noSimilar.set(b);
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
                LOGGER.info("Sending file to recycle bin: " + getFile());
                fu.moveToTrash(new File[]{getFile()});
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to send file to recycle bin: " + getFile(), e);
                LOGGER.info("Deleting file: " + getFile());
                return getFile().delete();
            }
        } else {
            LOGGER.info("Deleting file: " + getFile());
            return getFile().delete();
        }
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile() + "\"";
    }

}
