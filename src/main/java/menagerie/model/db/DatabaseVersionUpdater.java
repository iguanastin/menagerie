package menagerie.model.db;


import java.sql.*;
import java.util.ArrayList;


public class DatabaseVersionUpdater {


    private static final String CREATE_TAGS_TABLE_V1 = "CREATE TABLE tags(id INT PRIMARY KEY AUTO_INCREMENT, name NVARCHAR(128) NOT NULL UNIQUE);";
    private static final String CREATE_IMGS_TABLE_V1 = "CREATE TABLE imgs(id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, path NVARCHAR(1024) UNIQUE, added LONG NOT NULL, thumbnail BLOB, md5 NVARCHAR(32), histogram OBJECT);";
    private static final String CREATE_TAGGED_TABLE_V1 = "CREATE TABLE tagged(img_id INT NOT NULL, tag_id INT NOT NULL, FOREIGN KEY (img_id) REFERENCES imgs(id) ON DELETE CASCADE, FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, PRIMARY KEY (img_id, tag_id));";

    private static final int CURRENT_VERSION = 2;


    private static class Tag {
        int id;
        String name;
    }


    private static boolean outOfDate(Connection db) throws SQLException {
        return getVersion(db) != CURRENT_VERSION;
    }

    public static void updateDatabase(Connection db) throws SQLException {
        Statement s = db.createStatement();

        while (outOfDate(db)) {
            int version = getVersion(db);

            System.out.println("Database version: " + version);

            switch (version) {
                case -1:
                    cleanDatabase(db);
                    initializeTables(db);
                    break;
                case 0:
                    System.out.println("!!! Database needs to update from 0 to 1 !!!");
                    updateFromV0ToV1(db);
                    break;
                case 1:
                    System.out.println("!!! Database needs to update from 1 to 2 !!!");
                    updateFromV1ToV2(db);
                case 2:
                    System.out.println("Database is up to date");
                    break;
            }
        }

        s.close();
    }

    /**
     * Retrieves the version of the database.
     *
     * @param db Database connection
     * @return -1 if database hasn't been initialized. Int >= 0 if version data exists
     * @throws SQLException When database connection is bad or cannot create statement
     */
    private static int getVersion(Connection db) throws SQLException {
        Statement s = db.createStatement();

        int version;
        try {
            ResultSet rs = s.executeQuery("SELECT TOP 1 version.version FROM version ORDER BY version.version DESC;");
            if (rs.next()) {
                version = rs.getInt("version");
            } else {
                throw new DatabaseUpdateException("Version table has no version information.");
            }
        } catch (SQLException e) {
            //Database is either version 0 schema or not initialized
            try {
                s.executeQuery("SELECT TOP 1 * FROM imgs;");
                // Tables exist for version 0
                version = 0;
            } catch (SQLException e2) {
                // Tables don't exist or are not clean
                version = -1;
            }
        }

        s.close();

        return version;
    }

    private static void initializeTables(Connection db) throws SQLException {
        initializeV1Tables(db);
    }

    private static void initializeV1Tables(Connection db) throws SQLException {
        System.out.println("Initializing v1 tables");

        Statement s = db.createStatement();

        s.executeUpdate(CREATE_IMGS_TABLE_V1);
        System.out.println("  Initialized imgs table");
        s.executeUpdate(CREATE_TAGS_TABLE_V1);
        System.out.println("  Initialized tags table");
        s.executeUpdate(CREATE_TAGGED_TABLE_V1);
        System.out.println("  Initialized tagged table");
        s.executeUpdate("CREATE TABLE version(version INT NOT NULL PRIMARY KEY);");
        s.executeUpdate("INSERT INTO version(version) VALUES (1);");
        System.out.println("  Initialized version table and inserted current version");

        s.close();

        System.out.println("Finished initializing v1 tables");
    }

    private static void updateFromV0ToV1(Connection db) throws SQLException {
        System.out.println("Database updating from v0 to v1...");

        long t = System.currentTimeMillis();

        Statement s = db.createStatement();

        //------------------------ Set version in schema ---------------------------------------------------------------

        s.executeUpdate("CREATE TABLE version(version INT NOT NULL PRIMARY KEY);");
        s.executeUpdate("INSERT INTO version(version) VALUES (1);");

        //---------------------------- Add columns ---------------------------------------------------------------------

        s.executeUpdate("ALTER TABLE imgs ADD md5 NVARCHAR(32)");
        s.executeUpdate("ALTER TABLE imgs ADD thumbnail BLOB;");
        s.executeUpdate("ALTER TABLE imgs ADD histogram OBJECT;");

        //--------------------------------- Rename columns -------------------------------------------------------------

        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_id TO id;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_path TO path;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_added TO added;");

        //------------------------- Create tagging tables --------------------------------------------------------------

        s.executeUpdate(CREATE_TAGS_TABLE_V1);
        s.executeUpdate(CREATE_TAGGED_TABLE_V1);

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

                tagImage.setInt(1, rs_img.getInt("id"));
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

        //------------------------------------ Done Updating -----------------------------------------------------------

        s.close();

        System.out.println("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");

    }

    private static void updateFromV1ToV2(Connection db) throws SQLException {
        System.out.println("Database updating from v1 to v2...");
        long t = System.currentTimeMillis();
        Statement s = db.createStatement();

        //Update histogram storage
        s.executeUpdate("ALTER TABLE imgs DROP COLUMN histogram;" +
                "ALTER TABLE imgs ADD COLUMN hist_a BLOB;" +
                "ALTER TABLE imgs ADD COLUMN hist_r BLOB;" +
                "ALTER TABLE imgs ADD COLUMN hist_g BLOB;" +
                "ALTER TABLE imgs ADD COLUMN hist_b BLOB;");

        //Update version table
        s.executeUpdate("INSERT INTO version VALUES (2);");

        //------------------------------------ Done Updating -----------------------------------------------------------

        s.close();

        System.out.println("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
    }

    private static Tag getTagByName(Iterable<Tag> tags, String name) {
        for (Tag tag : tags) {
            if (tag.name.equalsIgnoreCase(name)) {
                return tag;
            }
        }

        return null;
    }

    private static void cleanDatabase(Connection db) throws SQLException {
        System.out.println("Cleaning database....");

        Statement s = db.createStatement();

        s.executeUpdate("DROP TABLE IF EXISTS imgs;");
        System.out.println("  Dropped table: imgs");
        s.executeUpdate("DROP TABLE IF EXISTS tags;");
        System.out.println("  Dropped table: tags");
        s.executeUpdate("DROP TABLE IF EXISTS tagged;");
        System.out.println("  Dropped table: tagged");
        s.executeUpdate("DROP TABLE IF EXISTS version;");
        System.out.println("  Dropped table: version");

        s.close();

        System.out.println("Finished cleaning database");
    }

}
