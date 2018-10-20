package menagerie.model;

import menagerie.model.db.DatabaseUpdater;
import menagerie.util.RunnableQueue;
import menagerie.model.search.SearchRule;

import java.io.File;
import java.sql.*;
import java.util.*;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";
    private static final String SQL_GET_IMG_TAG_IDS = "SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;";

    // ------------------------------ Variables -----------------------------------

    private List<ImageInfo> images = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    private Connection database;
    private RunnableQueue databaseUpdateQueue = new RunnableQueue();


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        // Update/verify database
        DatabaseUpdater.updateDatabaseIfNecessary(database);

        // Load data from database
        loadTagsFromDatabase(database);
        loadImagesFromDatabase(database);

        // Start runnable queue for database updates
        Thread thread = new Thread(databaseUpdateQueue);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadImagesFromDatabase(Connection database) throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_IMGS);

        while (rs.next()) {
            ImageInfo img = new ImageInfo(rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")), rs.getNString("md5"));
            images.add(img);

            PreparedStatement ps = database.prepareStatement(SQL_GET_IMG_TAG_IDS);
            ps.setInt(1, img.getId());
            ResultSet tagRS = ps.executeQuery();

            while (tagRS.next()) {
                img.getTags().add(getTagByID(tagRS.getInt("tag_id")));
            }
        }

        s.close();

        System.out.println("Finished loading " + images.size() + " images from database");
    }

    private void loadTagsFromDatabase(Connection database) throws SQLException {
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
