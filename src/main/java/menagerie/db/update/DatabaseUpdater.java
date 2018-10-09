package menagerie.db.update;


import java.sql.*;
import java.util.ArrayList;


public class DatabaseUpdater {


    private static class Tag {
        int id;
        String name;
    }

    public static void updateDatabaseIfNecessary(Connection db) throws SQLException {
        Statement s = db.createStatement();

        int major = 0;
        int minor = 0;
        try {
            ResultSet rs = s.executeQuery("SELECT TOP 1 version.major, version.minor FROM version ORDER BY version.major DESC, version.minor DESC;");
            if (rs.next()) {
                major = rs.getInt("major");
                minor = rs.getInt("minor");
            } else {
                throw new DatabaseUpdateException("Unknown database version. Version table is missing expected columns.");
            }
        } catch (SQLException e) {
            //No version table found, assume version 0
        }

        switch (major) {
            case 0:
                System.out.println("!!! Database needs to update from 0.0 to 1.0 !!!");
                updateToVersion1_0(db, s);
                break;
            case 1:
                System.out.println("Database is up to date");
                break;
        }
    }

    private static void updateToVersion1_0(Connection db, Statement s) throws SQLException {

        System.out.println("Database updating from 0.0 to 1.0...");
        long t = System.currentTimeMillis();

        //------------------------ Set version in schema ---------------------------------------------------------------

        s.executeUpdate("CREATE TABLE version(major INT NOT NULL, minor INT NOT NULL, PRIMARY KEY (major, minor));");
        s.executeUpdate("INSERT INTO version(major, minor) VALUES (1, 0);");

        //------------------------- Create tagging tables --------------------------------------------------------------

        s.executeUpdate("CREATE TABLE tags(id INT PRIMARY KEY AUTO_INCREMENT, name NVARCHAR(64) NOT NULL UNIQUE);");
        s.executeUpdate("CREATE TABLE tagged(img_id INT NOT NULL, tag_id INT NOT NULL, FOREIGN KEY (img_id) REFERENCES imgs(img_id), FOREIGN KEY (tag_id) REFERENCES tags(id), PRIMARY KEY (img_id, tag_id));");

        //------------------------------ Convert tags ------------------------------------------------------------------

        PreparedStatement s_createTag = db.prepareStatement("INSERT INTO tags(name) VALUES (?);");
        PreparedStatement s_getTagID = db.prepareStatement("SELECT tags.id FROM tags WHERE tags.name=?;");
        PreparedStatement tagImage = db.prepareStatement("INSERT INTO tagged VALUES (?, ?);");
        ResultSet rs_img = s.executeQuery("SELECT * FROM imgs;");

        final ArrayList<Tag> tags = new ArrayList<>();
        while (rs_img.next()) {
            String tagString = rs_img.getNString("img_tags");

            for (String tagName : tagString.trim().split(" ")) {
                Tag tag = getTagByName(tags, tagName);

                if (tag == null) {
                    s_createTag.setNString(1, tagName);
                    s_createTag.executeUpdate();
                    s_getTagID.setNString(1, tagName);
                    ResultSet rs_tag = s_getTagID.executeQuery();
                    rs_tag.next();
                    tag = new Tag();
                    tag.name = tagName;
                    tag.id = rs_tag.getInt("tags.id");
                    tags.add(tag);
                }

                tagImage.setInt(1, rs_img.getInt("img_id"));
                tagImage.setInt(2, tag.id);
                try {
                    tagImage.executeUpdate();
                } catch (SQLException e) {
                    //Image is already tagged with this tag
                }
            }
        }

        //--------------------------------- Remove columns -------------------------------------------------------------

        //Guaranteed
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN img_tags;");
        //Possible
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN IF EXISTS img_hist_alpha;");
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN IF EXISTS img_hist_red;");
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN IF EXISTS img_hist_green;");
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN IF EXISTS img_hist_blue;");
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN IF EXISTS img_src;");

        //---------------------------- Add columns ---------------------------------------------------------------------

        s.executeUpdate("ALTER TABLE imgs ADD thumbnail BLOB;");
        s.executeUpdate("ALTER TABLE imgs ADD histogram OBJECT;");

        //--------------------------------- Rename columns -------------------------------------------------------------

        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_id TO id;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_path TO path;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_added TO added;");

        //------------------------------------ Done Updating -----------------------------------------------------------

        System.out.println("Finished updating database in: " + (System.currentTimeMillis() - t)/1000.0 + "s");

    }

    private static Tag getTagByName(Iterable<Tag> tags, String name) {
        for (Tag tag : tags) {
            if (tag.name.equalsIgnoreCase(name)) {
                return tag;
            }
        }

        return null;
    }

    public static void main(String[] args) throws SQLException {
        Connection db = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
        DatabaseUpdater.updateDatabaseIfNecessary(db);
    }

}
