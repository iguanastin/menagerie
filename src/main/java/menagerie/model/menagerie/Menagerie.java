package menagerie.model.menagerie;

import menagerie.gui.Main;
import menagerie.model.db.DatabaseUpdateQueue;
import menagerie.model.search.Search;

import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";

    // -------------------------------- SQL Statements ----------------------------

    private final PreparedStatement PS_GET_IMG_TAG_IDS;
    private final PreparedStatement PS_GET_HIGHEST_IMG_ID;
    private final PreparedStatement PS_GET_HIGHEST_TAG_ID;
    private final PreparedStatement PS_DELETE_IMG;
    private final PreparedStatement PS_CREATE_IMG;
    private final PreparedStatement PS_DELETE_TAG;
    private final PreparedStatement PS_CREATE_TAG;
    final PreparedStatement PS_SET_IMG_MD5;
    final PreparedStatement PS_SET_IMG_HISTOGRAM;
    final PreparedStatement PS_SET_IMG_THUMBNAIL;
    final PreparedStatement PS_GET_IMG_THUMBNAIL;
    final PreparedStatement PS_ADD_TAG_TO_IMG;
    final PreparedStatement PS_REMOVE_TAG_FROM_IMG;

    // ------------------------------ Variables -----------------------------------

    private List<ImageInfo> images = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    private Map<String, ImageInfo> hashes = new HashMap<>();

    private int nextImageID;
    private int nextTagID;

    private Connection database;
    private DatabaseUpdateQueue updateQueue = new DatabaseUpdateQueue();

    private List<Search> activeSearches = new ArrayList<>();


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        //Initialize prepared database statements
        PS_GET_IMG_TAG_IDS = database.prepareStatement("SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;");
        PS_SET_IMG_MD5 = database.prepareStatement("UPDATE imgs SET imgs.md5=? WHERE imgs.id=?;");
        PS_SET_IMG_HISTOGRAM = database.prepareStatement("UPDATE imgs SET imgs.hist_a=?, imgs.hist_r=?, imgs.hist_g=?, imgs.hist_b=? WHERE imgs.id=?");
        PS_SET_IMG_THUMBNAIL = database.prepareStatement("UPDATE imgs SET imgs.thumbnail=? WHERE imgs.id=?;");
        PS_GET_IMG_THUMBNAIL = database.prepareStatement("SELECT imgs.thumbnail FROM imgs WHERE imgs.id=?;");
        PS_ADD_TAG_TO_IMG = database.prepareStatement("INSERT INTO tagged(img_id, tag_id) VALUES (?, ?);");
        PS_REMOVE_TAG_FROM_IMG = database.prepareStatement("DELETE FROM tagged WHERE img_id=? AND tag_id=?;");
        PS_GET_HIGHEST_IMG_ID = database.prepareStatement("SELECT TOP 1 imgs.id FROM imgs ORDER BY imgs.id DESC;");
        PS_GET_HIGHEST_TAG_ID = database.prepareStatement("SELECT TOP 1 tags.id FROM tags ORDER BY tags.id DESC;");
        PS_DELETE_IMG = database.prepareStatement("DELETE FROM imgs WHERE imgs.id=?;");
        PS_CREATE_IMG = database.prepareStatement("INSERT INTO imgs(id, path, added, md5, hist_a, hist_r, hist_g, hist_b) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
        PS_DELETE_TAG = database.prepareStatement("DELETE FROM tags WHERE tags.id=?;");
        PS_CREATE_TAG = database.prepareStatement("INSERT INTO tags(id, name) VALUES (?, ?);");

        // Load data from database
        loadTagsFromDatabase();
        loadImagesFromDatabase();
        clearUnusedTags();

        // Start runnable queue for database updates
        Thread thread = new Thread(updateQueue);
        thread.setDaemon(true);
        thread.start();

        initializeIdCounters();
    }

    private void clearUnusedTags() throws SQLException {
        Set<Integer> usedTags = new HashSet<>();
        for (ImageInfo img : images) {
            for (Tag t : img.getTags()) {
                usedTags.add(t.getId());
            }
        }
        for (Tag t : new ArrayList<>(tags)) {
            if (!usedTags.contains(t.getId())) {
                System.out.println("Deleting unused tag: " + t);

                tags.remove(t);

                PS_DELETE_TAG.setInt(1, t.getId());
                PS_DELETE_TAG.executeUpdate();
            }
        }
    }

    private void initializeIdCounters() throws SQLException {
        ResultSet rs = PS_GET_HIGHEST_IMG_ID.executeQuery();
        if (!rs.next()) nextImageID = 1;
        nextImageID = rs.getInt("id") + 1;
        rs.close();
        rs = PS_GET_HIGHEST_TAG_ID.executeQuery();
        if (!rs.next()) nextTagID = 1;
        nextTagID = rs.getInt("id") + 1;
        rs.close();
    }

    private void loadImagesFromDatabase() throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_IMGS);

        while (rs.next()) {
            ImageHistogram hist = null;

            InputStream histAlpha = rs.getBinaryStream("hist_a");
            if (histAlpha != null) {
                try {
                    hist = new ImageHistogram(histAlpha, rs.getBinaryStream("hist_r"), rs.getBinaryStream("hist_g"), rs.getBinaryStream("hist_b"));
                } catch (HistogramReadException e) {
                    System.out.println("Histogram failed to load from database:");
                    e.printStackTrace();
                }
            }

            ImageInfo img = new ImageInfo(this, rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")), rs.getNString("md5"), hist);
            images.add(img);

            if (img.getMD5() != null) {
                hashes.put(img.getMD5(), img);
            }

            PS_GET_IMG_TAG_IDS.setInt(1, img.getId());
            ResultSet tagRS = PS_GET_IMG_TAG_IDS.executeQuery();

            while (tagRS.next()) {
                img.getTags().add(getTagByID(tagRS.getInt("tag_id")));
            }
        }

        s.close();

        System.out.println("Finished loading " + images.size() + " images from database");
    }

    private void loadTagsFromDatabase() throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_TAGS);

        while (rs.next()) {
            tags.add(new Tag(this, rs.getInt("id"), rs.getNString("name")));
        }

        s.close();

        System.out.println("Finished loading " + tags.size() + " tags from database");
    }

    public ImageInfo importImage(File file, boolean computeMD5, boolean computeHistogram, boolean buildThumbnail) {
        if (isFilePresent(file)) {
            System.out.println("User tried to re-add existing file: " + file);
            return null;
        }

        ImageInfo img = new ImageInfo(this, nextImageID, System.currentTimeMillis(), file, null, null);

        //Compute md5 if flagged
        if (computeMD5) {
            img.initializeMD5();

            if (hashes.get(img.getMD5()) != null)
                return null;
            else
                hashes.put(img.getMD5(), img);
        }

        //Compute histogram if flagged
        if (computeHistogram) {
            img.initializeHistogram();
        }

        //Add image and commit to database
        images.add(img);
        nextImageID++;
        try {
            PS_CREATE_IMG.setInt(1, img.getId());
            PS_CREATE_IMG.setNString(2, img.getFile().getAbsolutePath());
            PS_CREATE_IMG.setLong(3, img.getDateAdded());
            PS_CREATE_IMG.setNString(4, img.getMD5());
            PS_CREATE_IMG.setBinaryStream(5, null);
            PS_CREATE_IMG.setBinaryStream(6, null);
            PS_CREATE_IMG.setBinaryStream(7, null);
            PS_CREATE_IMG.setBinaryStream(8, null);
            if (img.getHistogram() != null) {
                PS_CREATE_IMG.setBinaryStream(5, img.getHistogram().getAlphaAsInputStream());
                PS_CREATE_IMG.setBinaryStream(6, img.getHistogram().getRedAsInputStream());
                PS_CREATE_IMG.setBinaryStream(7, img.getHistogram().getGreenAsInputStream());
                PS_CREATE_IMG.setBinaryStream(8, img.getHistogram().getBlueAsInputStream());
            }
            PS_CREATE_IMG.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Tag with tagme
        img.addTag(getTagByName("tagme"));

        //Build thumbnail if flagged
        if (buildThumbnail) {
            img.getThumbnail();
        }

        //Update active searches
        activeSearches.forEach(search -> search.addIfValid(img));

        return img;
    }

    public void removeImage(ImageInfo image, boolean deleteFile) {
        if (image != null && images.remove(image)) {
            if (deleteFile) {
                if (!image.getFile().delete()) {
                    Main.showErrorMessage("Deletion Error", "Unable to delete file", image.getFile().toString());
                    return;
                }
            }

            if (image.getMD5() != null) {
                hashes.remove(image.getMD5());
            }

            activeSearches.forEach(search -> search.remove(image));
            updateQueue.enqueueUpdate(() -> {
                try {
                    PS_DELETE_IMG.setInt(1, image.getId());
                    PS_DELETE_IMG.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            updateQueue.commit();
        }
    }

    public List<Tag> getTags() {
        return tags;
    }

    private Tag getTagByID(int id) {
        for (Tag t : tags) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public Tag getTagByName(String name) {
        for (Tag t : tags) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    void imageTagsUpdated(ImageInfo img) {
        activeSearches.forEach(search -> search.removeIfInvalid(img));
    }

    void imageMD5Updated(ImageInfo img) {
        hashes.put(img.getMD5(), img);
    }

    public List<ImageInfo> getImages() {
        return images;
    }

    public DatabaseUpdateQueue getUpdateQueue() {
        return updateQueue;
    }

    private boolean isFilePresent(File file) {
        for (ImageInfo img : images) {
            if (img.getFile().equals(file)) return true;
        }
        return false;
    }

    public void closeSearch(Search search) {
        activeSearches.remove(search);
    }

    public void registerSearch(Search search) {
        activeSearches.add(search);
    }

    public Tag createTag(String name) {
        Tag t = new Tag(this, nextTagID, name);
        nextTagID++;

        tags.add(t);

        updateQueue.enqueueUpdate(() -> {
            try {
                PS_CREATE_TAG.setInt(1, t.getId());
                PS_CREATE_TAG.setNString(2, t.getName());
                PS_CREATE_TAG.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return t;
    }

}
