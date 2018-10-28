package menagerie.model;

import menagerie.gui.Main;
import menagerie.model.db.DatabaseUpdateQueue;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.search.Search;
import menagerie.model.search.SearchRule;

import java.io.File;
import java.sql.*;
import java.util.*;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";

    // -------------------------------- SQL Statements ----------------------------

    private final PreparedStatement PS_GET_IMG_TAG_IDS;
    private final PreparedStatement PS_GET_HIGHEST_IMG_ID;
    private final PreparedStatement PS_DELETE_IMG;
    private final PreparedStatement PS_CREATE_IMG;
    final PreparedStatement PS_SET_IMG_MD5;
    final PreparedStatement PS_SET_IMG_THUMBNAIL;
    final PreparedStatement PS_GET_IMG_THUMBNAIL;
    final PreparedStatement PS_ADD_TAG_TO_IMG;
    final PreparedStatement PS_REMOVE_TAG_FROM_IMG;

    // ------------------------------ Variables -----------------------------------

    private List<ImageInfo> images = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();
    Map<String, HashSet<ImageInfo>> knownMD5s = new HashMap<>();

    private Connection database;
    private DatabaseUpdateQueue updateQueue = new DatabaseUpdateQueue();

    private List<Search> activeSearches = new ArrayList<>();


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        //Initialize prepared database statements
        PS_GET_IMG_TAG_IDS = database.prepareStatement("SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;");
        PS_SET_IMG_MD5 = database.prepareStatement("UPDATE imgs SET imgs.md5=? WHERE imgs.id=?;");
        PS_SET_IMG_THUMBNAIL = database.prepareStatement("UPDATE imgs SET imgs.thumbnail=? WHERE imgs.id=?;");
        PS_GET_IMG_THUMBNAIL = database.prepareStatement("SELECT imgs.thumbnail FROM imgs WHERE imgs.id=?;");
        PS_ADD_TAG_TO_IMG = database.prepareStatement("INSERT INTO tagged(img_id, tag_id) VALUES (?, ?);");
        PS_REMOVE_TAG_FROM_IMG = database.prepareStatement("DELETE FROM tagged WHERE img_id=? AND tag_id=?;");
        PS_GET_HIGHEST_IMG_ID = database.prepareStatement("SELECT TOP 1 imgs.id FROM imgs ORDER BY imgs.id DESC;");
        PS_DELETE_IMG = database.prepareStatement("DELETE FROM imgs WHERE imgs.id=?;");
        PS_CREATE_IMG = database.prepareStatement("INSERT INTO imgs(id, path, added, md5) VALUES (?, ?, ?, ?);");

        // Load data from database
        loadTagsFromDatabase();
        loadImagesFromDatabase();

        // Start runnable queue for database updates
        Thread thread = new Thread(updateQueue);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadImagesFromDatabase() throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_IMGS);

        while (rs.next()) {
            ImageInfo img = new ImageInfo(this, rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")), rs.getNString("md5"));
            images.add(img);

            if (img.getMD5() != null) putMD5(img.getMD5(), img);

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
            tags.add(new Tag(rs.getInt("id"), rs.getNString("name")));
        }

        s.close();

        System.out.println("Finished loading " + tags.size() + " tags from database");
    }

    public ImageInfo importImage(File file, boolean computeMD5, boolean computeHistogram, boolean buildThumbnail) {
        ImageInfo img = new ImageInfo(this, getNextAvailableImageID(), System.currentTimeMillis(), file, null);
        if (computeMD5) {
            img.initializeMD5();
            if (knownMD5s.get(img.getMD5()).size() > 1) {
                //TODO: Handle duplicate image
                Main.showErrorMessage("Duplicate image already exists", "An image already exists with the same MD5 hash", "Existing image ID: " + knownMD5s.get(img.getMD5()));
                //TODO: Remove duplicate md5 from hashmap
                return null;
            }
        }
        if (computeHistogram) {
            //TODO: Compute histogram
        }

        images.add(img);
        activeSearches.forEach(search -> search.addIfValid(img));
        updateQueue.enqueueUpdate(() -> {
            try {
                PS_CREATE_IMG.setInt(1, img.getId());
                PS_CREATE_IMG.setNString(2, img.getFile().getAbsolutePath());
                PS_CREATE_IMG.setLong(3, img.getDateAdded());
                PS_CREATE_IMG.setNString(4, img.getMD5());
                PS_CREATE_IMG.executeUpdate();

                PS_ADD_TAG_TO_IMG.setInt(1, img.getId());
                PS_ADD_TAG_TO_IMG.setInt(2, getTagByName("tagme").getId());
                PS_ADD_TAG_TO_IMG.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (buildThumbnail) {
                img.getThumbnail();
            }
        });
        updateQueue.commit();

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

    void putMD5(String hash, ImageInfo image) {
        if (knownMD5s.get(hash) == null) {
            knownMD5s.put(hash, new HashSet<>(Collections.singletonList(image)));
        } else {
            knownMD5s.get(hash).add(image);
        }
    }

    private int getNextAvailableImageID() {
        try {
            ResultSet rs = PS_GET_HIGHEST_IMG_ID.executeQuery();

            if (!rs.next()) return -1;

            return rs.getInt("id") + 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
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

    public List<Tag> getTagsByNames(List<String> names) {
        List<Tag> results = new ArrayList<>();

        for (String name : names) {
            Tag t = getTagByName(name);
            if (t != null) {
                results.add(t);
            } else {
                results.add(new Tag(-1, name));
            }
        }

        return results;
    }

    public List<ImageInfo> getImages() {
        return images;
    }

    public DatabaseUpdateQueue getUpdateQueue() {
        return updateQueue;
    }

    public void closeSearch(Search search) {
        activeSearches.remove(search);
    }

    public void registerSearch(Search search) {
        activeSearches.add(search);
    }
}
