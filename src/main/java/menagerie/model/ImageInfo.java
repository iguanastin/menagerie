package menagerie.model;

import javafx.scene.image.Image;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageInfo {

    // ---------------------------- Constants ----------------------------------------

    private static final int THUMBNAIL_SIZE = 150;

    // -------------------------------- Variables ------------------------------------

    private final int id;
    private final long dateAdded;
    private final File file;
    private final List<Tag> tags = new ArrayList<>();

    private byte[] md5;

    private SoftReference<Image> thumbnail;
    private SoftReference<Image> image;


    public ImageInfo(int id, long dateAdded, File file) {
        this.id = id;
        this.dateAdded = dateAdded;
        this.file = file;
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
        Image img = thumbnail.get();
        if (img == null) {
            img = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
            //TODO: Save thumbnail to database?
        }
        return img;
    }

    public Image getImage() {
        Image img = image.get();
        if (img == null) {
            img = new Image(file.toURI().toString());
            image = new SoftReference<>(img);
        }
        return img;
    }

    public boolean hasTag(String name) {
        for (Tag tag : tags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "Image (" + getId() + ") \"" + getFile().getAbsolutePath() + "\" - " + new Date(getDateAdded());
    }

}
