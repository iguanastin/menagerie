package menagerie.model;

import menagerie.model.db.DatabaseUpdateQueue;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.search.SearchRule;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";

    // -------------------------------- SQL Statements ----------------------------

    private final PreparedStatement PS_GET_IMG_TAG_IDS;
    public final PreparedStatement PS_SET_IMG_MD5;
    public final PreparedStatement PS_SET_IMG_THUMBNAIL;
    public final PreparedStatement PS_GET_IMG_THUMBNAIL;
    public final PreparedStatement PS_ADD_TAG_TO_IMG;

    // ------------------------------ Variables -----------------------------------

    private List<ImageInfo> images = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    private Connection database;
    private DatabaseUpdateQueue updateQueue = new DatabaseUpdateQueue();


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        // Update/verify database
        DatabaseVersionUpdater.updateDatabaseIfNecessary(database);

        //Initialize prepared database statements
        PS_GET_IMG_TAG_IDS = database.prepareStatement("SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;");
        PS_SET_IMG_MD5 = database.prepareStatement("UPDATE imgs SET imgs.md5=? WHERE imgs.id=?;");
        PS_SET_IMG_THUMBNAIL = database.prepareStatement("UPDATE imgs SET imgs.thumbnail=? WHERE imgs.id=?;");
        PS_GET_IMG_THUMBNAIL = database.prepareStatement("SELECT imgs.thumbnail FROM imgs WHERE imgs.id=?;");
        PS_ADD_TAG_TO_IMG = database.prepareStatement("INSERT INTO tagged(img_id, tag_id) VALUES (?, ?);");

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

    public DatabaseUpdateQueue getUpdateQueue() {
        return updateQueue;
    }

    public List<ImageInfo> searchImages(List<SearchRule> rules, boolean descending) {
        List<ImageInfo> results = new ArrayList<>();

        rules.sort(null);

        rules.forEach(System.out::println);

        for (ImageInfo img : images) {
            if (imageFitsSearch(img, rules)) results.add(img);
        }

        results.sort((o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        });

        return results;
    }

    private boolean imageFitsSearch(ImageInfo img, List<SearchRule> rules) {
        if (rules != null) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) return false;
            }
        }

        return true;
    }

}
