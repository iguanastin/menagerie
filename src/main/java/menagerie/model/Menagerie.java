package menagerie.model;

import menagerie.model.db.DatabaseUpdater;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Menagerie {

    // ----------------------------- Constants ------------------------------------

    private static final String SQL_GET_TAGS = "SELECT tags.* FROM tags;";
    private static final String SQL_GET_IMGS = "SELECT imgs.* FROM imgs;";
    private static final String SQL_GET_IMG_TAG_IDS = "SELECT tagged.tag_id FROM tagged JOIN imgs ON tagged.img_id=imgs.id WHERE imgs.id=?;";

    // ------------------------------ Variables -----------------------------------

    private List<ImageInfo> images = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    private Connection database;


    public Menagerie(Connection database) throws SQLException {
        this.database = database;

        DatabaseUpdater.updateDatabaseIfNecessary(database);

        loadTagsFromDatabase(database);
        loadImagesFromDatabase(database);
    }

    private void loadImagesFromDatabase(Connection database) throws SQLException {
        Statement s = database.createStatement();
        ResultSet rs = s.executeQuery(SQL_GET_IMGS);

        while (rs.next()) {
            ImageInfo img = new ImageInfo(rs.getInt("id"), rs.getLong("added"), new File(rs.getNString("path")));
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

}
