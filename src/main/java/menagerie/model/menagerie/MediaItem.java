package menagerie.model.menagerie;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.scene.image.Image;
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

public class MediaItem extends Item {

    // -------------------------------- Variables ------------------------------------

    private File file;
    private String md5;
    private ImageHistogram histogram;

    private SoftReference<Thumbnail> thumbnail;
    private WeakReference<Image> image;

    private GroupItem group;


    MediaItem(Menagerie menagerie, int id, long dateAdded, File file, String md5, ImageHistogram histogram) {
        super(menagerie, id, dateAdded);
        this.file = file;
        this.md5 = md5;
        this.histogram = histogram;
    }

    public File getFile() {
        return file;
    }

    @Override
    public Thumbnail getThumbnail() {
        Thumbnail thumb = null;
        if (thumbnail != null) thumb = thumbnail.get();
        if (thumb == null) {
            try {
                thumb = menagerie.getDatabaseUpdater().getThumbnail(getId());
                if (thumb != null) thumbnail = new SoftReference<>(thumb);
            } catch (SQLException e) {
                System.err.println("Failed to get thumbnail from database: " + getId());
                e.printStackTrace();
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
                    menagerie.getDatabaseUpdater().setThumbnailAsync(getId(), thumb.getImage());
                } else {
                    thumb.setImageLoadedListener(image1 -> menagerie.getDatabaseUpdater().setThumbnailAsync(getId(), image1));
                }
            }
        }

        return thumb;
    }

    public Image getImage() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString(), true);
            image = new WeakReference<>(img);
        }
        return img;
    }

    private Image getImageAsync() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString());
            image = new WeakReference<>(img);
        } else if (img.isBackgroundLoading() && img.getProgress() != 1) {
            img = new Image(file.toURI().toString());
        }
        return img;
    }

    public String getMD5() {
        return md5;
    }

    public ImageHistogram getHistogram() {
        return histogram;
    }

    public boolean isImage() {
        return Filters.IMAGE_NAME_FILTER.accept(file);
    }

    public boolean isVideo() {
        return Filters.VIDEO_NAME_FILTER.accept(file);
    }

    public void initializeMD5() {
        if (md5 != null) return;

        try {
            md5 = HexBin.encode(MD5Hasher.hash(getFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void commitMD5ToDatabase() {
        if (md5 == null) return;
        menagerie.getDatabaseUpdater().setMD5Async(getId(), md5);
    }

    public void initializeHistogram() {
        try {
            histogram = new ImageHistogram(getImageAsync());
        } catch (HistogramReadException ignore) {
        }
    }

    public void commitHistogramToDatabase() {
        if (histogram == null) return;
        menagerie.getDatabaseUpdater().setHistAsync(getId(), histogram);
    }

    public boolean renameTo(File dest) {
        if (file.equals(dest)) return true;

        boolean succeeded = file.renameTo(dest);

        if (succeeded) {
            file = dest;

            try {
                menagerie.getDatabaseUpdater().setPath(getId(), file.getAbsolutePath());
            } catch (SQLException e) {
                System.err.println("Failed to update new path to file");
                e.printStackTrace();
            }
        }

        return succeeded;
    }

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

    void setGroup(GroupItem group) {
        this.group = group;
        //TODO update database
    }

    public boolean inGroup() {
        return group != null;
    }

    public GroupItem getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\"";
    }

}
