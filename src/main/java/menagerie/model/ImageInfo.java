package menagerie.model;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import javafx.scene.image.Image;
import menagerie.util.MD5Hasher;
import menagerie.util.ThumbnailBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageInfo implements Comparable<ImageInfo> {

    // ---------------------------- Constants ----------------------------------------

    public static final int THUMBNAIL_SIZE = 150;

    // -------------------------------- Variables ------------------------------------

    private final Menagerie menagerie;

    private final int id;
    private final long dateAdded;
    private final File file;
    private final List<Tag> tags = new ArrayList<>();

    private String md5;

    private SoftReference<Image> thumbnail;
    private SoftReference<Image> image;


    public ImageInfo(Menagerie menagerie, int id, long dateAdded, File file, String md5) {
        this.menagerie = menagerie;
        this.id = id;
        this.dateAdded = dateAdded;
        this.file = file;
        this.md5 = md5;
    }

    public File getFile() {
        return file;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public int getId() {
        return id;
    }

    public Image getThumbnail() {
        Image img = null;
        if (thumbnail != null) img = thumbnail.get();
        if (img == null) {
            img = makeThumbnail(getFile());
            thumbnail = new SoftReference<>(img);
            //TODO: Save thumbnail to database?
        }
        return img;
    }

    public Image getImage() {
        Image img = null;
        if (image != null) img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString());
            image = new SoftReference<>(img);
        }
        return img;
    }

    public String getMD5() {
        if (md5 == null) {
            try {
                md5 = HexBin.encode(MD5Hasher.hash(getFile()));

                if (md5 != null) {
                    menagerie.getUpdateQueue().enqueueUpdate(() -> {
                        try {
                            menagerie.PS_SET_IMG_MD5.setNString(1, md5);
                            menagerie.PS_SET_IMG_MD5.setInt(2, id);
                            menagerie.PS_SET_IMG_MD5.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return md5;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Menagerie getMenagerie() {
        return menagerie;
    }

    public boolean hasTag(Tag t) {
        return getTags().contains(t);
    }

    public boolean addTag(Tag t) {
        if (hasTag(t)) return false;
        tags.add(t);

        //TODO: Queue db update

        return true;
    }

    public boolean removeTag(Tag t) {
        if (!hasTag(t)) return false;
        tags.remove(t);

        //TODO: Queue db update

        return true;
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\" - " + new Date(getDateAdded());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ImageInfo && ((ImageInfo) obj).getId() == getId();
    }

    @Override
    public int compareTo(ImageInfo o) {
        return getId() - o.getId();
    }

    private static Image makeThumbnail(File file) {
        String extension = file.getName().toLowerCase();
        extension = extension.substring(extension.indexOf('.') + 1);

        switch (extension) {
            case "png":
            case "jpg":
            case "jpeg":
            case "bmp":
                return new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            case "gif":
                //TODO: Make thumbnail still and have "GIF" text applied
                return new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            case "webm":
            case "mp4":
            case "avi":
                //TODO: Load video into VLCJ player and take snapshot
                return null;
            default:
                return null;
        }
    }

}
