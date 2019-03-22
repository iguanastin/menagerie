package menagerie.model.menagerie;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.scene.image.Image;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.menagerie.histogram.HistogramReadException;
import menagerie.model.menagerie.histogram.ImageHistogram;
import menagerie.util.Filters;
import menagerie.util.ImageInputStreamConverter;
import menagerie.util.MD5Hasher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class MediaItem extends Item {

    // -------------------------------- Variables ------------------------------------

    private File file;
    private String md5;
    private ImageHistogram histogram;

    private SoftReference<Thumbnail> thumbnail;
    private WeakReference<Image> image;


    public MediaItem(Menagerie menagerie, int id, long dateAdded, File file, String md5, ImageHistogram histogram) {
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
                menagerie.PS_GET_IMG_THUMBNAIL.setInt(1, id);
                ResultSet rs = menagerie.PS_GET_IMG_THUMBNAIL.executeQuery();
                if (rs.next()) {
                    InputStream binaryStream = rs.getBinaryStream("thumbnail");
                    if (binaryStream != null) {
                        thumb = new Thumbnail(ImageInputStreamConverter.imageFromInputStream(binaryStream));
                        thumbnail = new SoftReference<>(thumb);
                    }
                }
            } catch (SQLException e) {
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
                final Thumbnail finalThumb = thumb;
                Runnable update = () -> {
                    try {
                        menagerie.PS_SET_IMG_THUMBNAIL.setBinaryStream(1, ImageInputStreamConverter.imageToInputStream(finalThumb.getImage()));
                        menagerie.PS_SET_IMG_THUMBNAIL.setInt(2, id);
                        menagerie.PS_SET_IMG_THUMBNAIL.executeUpdate();
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                };

                if (thumb.isLoaded()) {
                    menagerie.getUpdateQueue().enqueueUpdate(update);
                    menagerie.getUpdateQueue().commit();
                } else {
                    thumb.setImageLoadedListener(image1 -> {
                        menagerie.getUpdateQueue().enqueueUpdate(update);
                        menagerie.getUpdateQueue().commit();
                    });
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

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_SET_IMG_MD5.setNString(1, md5);
                menagerie.PS_SET_IMG_MD5.setInt(2, id);
                menagerie.PS_SET_IMG_MD5.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        menagerie.getUpdateQueue().commit();

    }

    public void initializeHistogram() {
        try {
            histogram = new ImageHistogram(getImageAsync());
        } catch (HistogramReadException e) {
            // A comment to make the warning of an empty catch block go away
        }
    }

    public void commitHistogramToDatabase() {
        if (histogram == null) return;

        menagerie.getUpdateQueue().enqueueUpdate(() -> {
            try {
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(1, histogram.getAlphaAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(2, histogram.getRedAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(3, histogram.getGreenAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setBinaryStream(4, histogram.getBlueAsInputStream());
                menagerie.PS_SET_IMG_HISTOGRAM.setInt(5, id);
                menagerie.PS_SET_IMG_HISTOGRAM.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

    }

    public boolean renameTo(File dest) {
        if (file.equals(dest)) return true;

        boolean succeeded = file.renameTo(dest);

        if (succeeded) {
            file = dest;

            menagerie.getUpdateQueue().enqueueUpdate(() -> {
                try {
                    menagerie.PS_SET_IMG_PATH.setNString(1, file.getAbsolutePath());
                    menagerie.PS_SET_IMG_PATH.setInt(2, id);
                    menagerie.PS_SET_IMG_PATH.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            menagerie.getUpdateQueue().commit();
        }

        return succeeded;
    }

    public double getSimilarityTo(MediaItem other, boolean compareBlackAndWhiteHists) {
        if (md5 != null && md5.equals(other.getMD5())) {
            return 1.0;
        } else if (histogram != null && other.getHistogram() != null) {
            if (compareBlackAndWhiteHists || (!histogram.isBlackAndWhite() && !other.getHistogram().isBlackAndWhite())) {
                return histogram.getSimilarity(other.getHistogram());
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\" - " + new Date(getDateAdded());
    }

}
