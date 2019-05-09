/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model.menagerie.db;


import menagerie.gui.Main;
import menagerie.model.menagerie.Tag;

import java.sql.*;
import java.util.ArrayList;

/**
 * Utility class for initializing and upgrading the Menagerie database.
 */
public class DatabaseVersionUpdater {

    private static final String DROP_TABLES = "DROP TABLE IF EXISTS imgs; DROP TABLE IF EXISTS tags; DROP TABLE IF EXISTS tagged; DROP TABLE IF EXISTS version; DROP TABLE IF EXISTS items; DROP TABLE IF EXISTS groups; DROP TABLE IF EXISTS media;";
    private static final String CREATE_VERSION_TABLE = "CREATE TABLE version(version INT NOT NULL PRIMARY KEY);";

    private static final String CREATE_TAGS_TABLE_V1 = "CREATE TABLE tags(id INT PRIMARY KEY AUTO_INCREMENT, name NVARCHAR(128) NOT NULL UNIQUE);";
    private static final String CREATE_IMGS_TABLE_V1 = "CREATE TABLE imgs(id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, path NVARCHAR(1024) UNIQUE, added LONG NOT NULL, thumbnail BLOB, md5 NVARCHAR(32), histogram OBJECT);";
    private static final String CREATE_TAGGED_TABLE_V1 = "CREATE TABLE tagged(img_id INT NOT NULL, tag_id INT NOT NULL, FOREIGN KEY (img_id) REFERENCES imgs(id) ON DELETE CASCADE, FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, PRIMARY KEY (img_id, tag_id));";

    private static final String CREATE_TAGS_TABLE_V3 = CREATE_TAGS_TABLE_V1;
    private static final String CREATE_ITEMS_TABLE_V3 = "CREATE TABLE items(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, added LONG NOT NULL);";
    private static final String CREATE_TAGGED_TABLE_V3 = "CREATE TABLE tagged(item_id INT NOT NULL, tag_id INT NOT NULL, FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, PRIMARY KEY (item_id, tag_id));";
    private static final String CREATE_GROUPS_TABLE_V3 = "CREATE TABLE groups(id INT NOT NULL PRIMARY KEY, title NVARCHAR(1024), FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE);";
    private static final String CREATE_MEDIA_TABLE_V3 = "CREATE TABLE media(id INT NOT NULL PRIMARY KEY, gid INT, path NVARCHAR(1024) UNIQUE, md5 NVARCHAR(32), thumbnail BLOB, hist_a BLOB, hist_r BLOB, hist_g BLOB, hist_b BLOB, FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE, FOREIGN KEY (gid) REFERENCES groups(id) ON DELETE SET NULL);";

    private static final String CREATE_TAG_NOTES_TABLE_V4 = "CREATE TABLE tag_notes(tag_id INT, note NVARCHAR(1024), FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE);";


    /**
     * Attempts to upgrade the database if it is out of date.
     *
     * @param db Database
     * @throws SQLException If the upgrade fails.
     */
    public static void updateDatabase(Connection db) throws SQLException {
        int version = getVersion(db);

        Main.log.info("Found database version: " + version);

        if (version == -1) {
            cleanDatabase(db);
            version = initializeTables(db);
        }
        if (version == 0) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV0ToV1(db);
            version++;
        }
        if (version == 1) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV1ToV2(db);
            version++;
        }
        if (version == 2) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV2ToV3(db);
            version++;
        }
        if (version == 3) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV3ToV4(db);
            version++;
        }
        if (version == 4) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV4ToV5(db);
            version++;
        }
        if (version == 5) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV5ToV6(db);
            version++;
        }
        if (version == 6) {
            Main.log.warning("!!! Database needs to update from v" + version + " to v" + (version + 1) + " !!!");
            updateFromV6ToV7(db);
            version++;
        }
        if (version == 7) {
            Main.log.info("Database is up to date");
        }
    }

    /**
     * Retrieves the version of the database.
     *
     * @param db Database connection
     * @return -1 if database hasn't been initialized. Int >= 0 if version data exists
     * @throws SQLException When database connection is bad or cannot create statement
     */
    private static int getVersion(Connection db) throws SQLException {
        try (Statement s = db.createStatement()) {

            int version;
            try (ResultSet rs = s.executeQuery("SELECT TOP 1 version.version FROM version ORDER BY version.version DESC;")) {
                if (rs.next()) {
                    version = rs.getInt("version");
                } else {
                    throw new DatabaseUpdateException("Version table has no version information.");
                }
            } catch (SQLException e) {
                //Database is either version 0 schema or not initialized
                try (ResultSet ignore = s.executeQuery("SELECT TOP 1 * FROM imgs;")) {
                    // Tables exist for version 0
                    version = 0;
                } catch (SQLException e2) {
                    // Tables don't exist or are not clean
                    version = -1;
                }
            }

            return version;
        }

    }

    /**
     * Utility function to initialize tables to the target version.
     *
     * @param db Database
     * @throws SQLException If database initialization fails.
     */
    private static int initializeTables(Connection db) throws SQLException {
        initializeV3Tables(db);
        return 3;
    }

    /**
     * Initializes the tables to version 1.
     *
     * @param db Database
     * @throws SQLException If database initialization fails.
     */
    private static void initializeV1Tables(Connection db) throws SQLException {
        Main.log.info("Initializing v1 tables...");

        try (Statement s = db.createStatement()) {
            s.executeUpdate(CREATE_IMGS_TABLE_V1);
            Main.log.info("  Initialized imgs table");
            s.executeUpdate(CREATE_TAGS_TABLE_V1);
            Main.log.info("  Initialized tags table");
            s.executeUpdate(CREATE_TAGGED_TABLE_V1);
            Main.log.info("  Initialized tagged table");
            s.executeUpdate(CREATE_VERSION_TABLE);
            s.executeUpdate("INSERT INTO version(version) VALUES (1);");
            Main.log.info("  Initialized version table and inserted current version");

            Main.log.info("Finished initializing v1 tables");
        }
    }

    /**
     * Initializes tables to version 3. Expects a clean database to work from.
     *
     * @param db Database
     * @throws SQLException If database initialization fails.
     */
    private static void initializeV3Tables(Connection db) throws SQLException {
        Main.log.info("Initializing v3 tables...");

        try (Statement s = db.createStatement()) {
            s.executeUpdate(CREATE_TAGS_TABLE_V3);
            Main.log.info("  Initialized tags table");
            s.executeUpdate(CREATE_ITEMS_TABLE_V3);
            Main.log.info("  Initialized items table");
            s.executeUpdate(CREATE_TAGGED_TABLE_V3);
            Main.log.info("  Initialized tagged table");
            s.executeUpdate(CREATE_GROUPS_TABLE_V3);
            Main.log.info("  Initialized groups table");
            s.executeUpdate(CREATE_MEDIA_TABLE_V3);
            Main.log.info("  Initialized media table");
            s.executeUpdate(CREATE_VERSION_TABLE);
            s.executeUpdate("INSERT INTO version(version) VALUES (3);");
            Main.log.info("  Initialized version table and inserted current version");

            Main.log.info("Finished initializing v3 tables");
        }
    }

    /**
     * Updates version 0 tables to version 1.
     *
     * @param db Database
     * @throws SQLException If database upgrade fails.
     */
    private static void updateFromV0ToV1(Connection db) throws SQLException {
        Main.log.warning("Database updating from v0 to v1...");

        long t = System.currentTimeMillis();

        Statement s = db.createStatement();

        //------------------------ Set version in schema ---------------------------------------------------------------

        Main.log.info("Updating database version");
        s.executeUpdate("CREATE TABLE version(version INT NOT NULL PRIMARY KEY);");
        s.executeUpdate("INSERT INTO version(version) VALUES (1);");

        //---------------------------- Add columns ---------------------------------------------------------------------

        Main.log.info("Modifying database columns");
        s.executeUpdate("ALTER TABLE imgs ADD md5 NVARCHAR(32)");
        s.executeUpdate("ALTER TABLE imgs ADD thumbnail BLOB;");
        s.executeUpdate("ALTER TABLE imgs ADD histogram OBJECT;");

        //--------------------------------- Rename columns -------------------------------------------------------------

        Main.log.info("Renaming database columns");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_id TO id;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_path TO path;");
        s.executeUpdate("ALTER TABLE imgs RENAME COLUMN img_added TO added;");

        //------------------------- Create tagging tables --------------------------------------------------------------

        Main.log.info("Creating tagging tables");
        s.executeUpdate(CREATE_TAGS_TABLE_V1);
        s.executeUpdate(CREATE_TAGGED_TABLE_V1);

        //------------------------------ Convert tags ------------------------------------------------------------------

        Main.log.info("Converting database tags format");
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
                    tag = new Tag(null, rs_tag.getInt("tags.id"), tagName, null);
                    tags.add(tag);
                }

                tagImage.setInt(1, rs_img.getInt("id"));
                tagImage.setInt(2, tag.getId());
                try {
                    tagImage.executeUpdate();
                } catch (SQLException e) {
                    //Image is already tagged with this tag
                }
            }
        }

        //--------------------------------- Remove columns -------------------------------------------------------------

        Main.log.info("Removing database columns");
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

        Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");

    }

    /**
     * Upgrades version 1 tables and data to version 2.
     *
     * @param db Database
     * @throws SQLException If database upgrade fails part way.
     */
    private static void updateFromV1ToV2(Connection db) throws SQLException {
        Main.log.warning("Database updating from v1 to v2...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Altering database histogram columns");
            //Update histogram storage
            s.executeUpdate("ALTER TABLE imgs DROP COLUMN histogram;" + "ALTER TABLE imgs ADD COLUMN hist_a BLOB;" + "ALTER TABLE imgs ADD COLUMN hist_r BLOB;" + "ALTER TABLE imgs ADD COLUMN hist_g BLOB;" + "ALTER TABLE imgs ADD COLUMN hist_b BLOB;");

            Main.log.info("Updating database version");
            //Update version table
            s.executeUpdate("INSERT INTO version(version) VALUES (2);");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }
    }

    /**
     * Upgrades version 2 tables and data to version 3.
     *
     * @param db Database
     * @throws SQLException If database upgrade fails part way.
     */
    private static void updateFromV2ToV3(Connection db) throws SQLException {
        Main.log.warning("Database updating from v2 to v3...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Extracting 'id' and 'added' from 'imgs' to new table 'items'");
            s.executeUpdate("CREATE TABLE items AS SELECT id, added FROM imgs;");

            Main.log.info("Removing extraneous 'added' column");
            s.executeUpdate("ALTER TABLE imgs DROP COLUMN added;");

            Main.log.info("Fixing tag table foreign keys");
            s.executeUpdate("CREATE TABLE new_tagged(img_id INT NOT NULL, tag_id INT NOT NULL, FOREIGN KEY (img_id) REFERENCES items(id) ON DELETE CASCADE, FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE, PRIMARY KEY (img_id, tag_id)) AS SELECT img_id, tag_id FROM tagged;");
            s.executeUpdate("DROP TABLE tagged;");
            s.executeUpdate("ALTER TABLE new_tagged RENAME TO tagged;");
            s.executeUpdate("ALTER TABLE tagged RENAME COLUMN img_id TO item_id;");

            Main.log.info("Creating groups table");
            s.executeUpdate(CREATE_GROUPS_TABLE_V3);

            Main.log.info("Renaming 'imgs' to 'media' and adding constraints/columns");
            s.executeUpdate("ALTER TABLE imgs RENAME TO media;");
            s.executeUpdate("ALTER TABLE media ADD COLUMN gid INT;");
            s.executeUpdate("ALTER TABLE media ADD FOREIGN KEY (id) REFERENCES items(id) ON DELETE CASCADE;");
            s.executeUpdate("ALTER TABLE media ADD FOREIGN KEY (gid) REFERENCES groups(id) ON DELETE SET NULL;");

            Main.log.info("Setting database version");
            s.executeUpdate("INSERT INTO version(version) VALUES (3);");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }
    }

    private static void updateFromV3ToV4(Connection db) throws SQLException {
        Main.log.warning("Database updating from v3 to v4...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Adding media page column");
            s.executeUpdate("ALTER TABLE media ADD COLUMN page INT NOT NULL DEFAULT 0;");

            Main.log.info("Creating 'tag_notes' table");
            s.executeUpdate(CREATE_TAG_NOTES_TABLE_V4);

            Main.log.info("Adding 'color' column to 'tags' table");
            s.executeUpdate("ALTER TABLE tags ADD COLUMN color NVARCHAR(32);");

            Main.log.info("Setting database version");
            s.executeUpdate("INSERT INTO version(version) VALUES (4)");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }
    }

    private static void updateFromV4ToV5(Connection db) throws SQLException {
        Main.log.warning("Database updating from v4 to v5...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Fixing 'tag_notes' foreign key");
            s.executeUpdate("CREATE TABLE tag_notes2(tag_id INT, note NVARCHAR(1024), FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE) AS SELECT tag_id, note FROM tag_notes;");
            s.executeUpdate("DROP TABLE tag_notes;");
            s.executeUpdate("ALTER TABLE tag_notes2 RENAME TO tag_notes;");

            Main.log.info("Setting database version");
            s.executeUpdate("INSERT INTO version(version) VALUES (5);");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }
    }

    private static void updateFromV5ToV6(Connection db) throws SQLException {
        Main.log.warning("Database updating from v5 to v6...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Adding 'no_similar' column to 'media'");
            s.executeUpdate("ALTER TABLE media ADD COLUMN no_similar BOOL NOT NULL DEFAULT FALSE;");

            Main.log.info("Setting database version");
            s.executeUpdate("INSERT INTO version(version) VALUES (6);");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }
    }

    private static Connection updateFromV6ToV7(Connection db) throws SQLException {
        Main.log.warning("Database updating from v5 to v6...");
        long t = System.currentTimeMillis();
        try (Statement s = db.createStatement()) {
            Main.log.info("Dropping thumbnail column from media");
            s.executeUpdate("ALTER TABLE media DROP COLUMN thumbnail;");

            Main.log.info("Setting database version");
            s.executeUpdate("INSERT INTO version(version) VALUES (7);");

            Main.log.info("Finished updating database in: " + (System.currentTimeMillis() - t) / 1000.0 + "s");
        }

        return db;
    }

    /**
     * @param tags List of available tags to get from.
     * @param name Name of tag to get.
     * @return Tag with given name from list, or null if not present.
     */
    private static Tag getTagByName(Iterable<Tag> tags, String name) {
        for (Tag tag : tags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }

        return null;
    }

    /**
     * Drops all tables and data, theoretically leaving the database as clean as when it was created.
     *
     * @param db Database
     * @throws SQLException If database cleaning fails.
     */
    private static void cleanDatabase(Connection db) throws SQLException {
        Main.log.warning("Cleaning database....");

        try (Statement s = db.createStatement()) {
            Main.log.info("Dropping database tables");
            s.executeUpdate(DROP_TABLES);

            Main.log.info("Finished cleaning database");
        }
    }


    public static void main(String[] args) throws SQLException {
        // Copies all data into new database

        Connection db1 = DriverManager.getConnection("jdbc:h2:~/test-purge", "sa", "");
        Connection db2 = DriverManager.getConnection("jdbc:h2:~/test-purge-new", "sa", "");

        Statement s1 = db1.createStatement();
        Statement s2 = db2.createStatement();

        updateDatabase(db2);

        ResultSet rs = s1.executeQuery("SELECT * FROM tags;");
        PreparedStatement ps = db2.prepareStatement("INSERT INTO tags VALUES (?, ?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt(1));
            ps.setNString(2, rs.getNString(2));
            ps.setNString(3, rs.getNString(3));
            ps.executeUpdate();
        }

        rs = s1.executeQuery("SELECT * FROM tag_notes;");
        ps = db2.prepareStatement("INSERT INTO tag_notes VALUES (?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt(1));
            ps.setNString(2, rs.getNString(2));
            ps.executeUpdate();
        }

        rs = s1.executeQuery("SELECT * FROM items;");
        ps = db2.prepareStatement("INSERT INTO items VALUES (?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt(1));
            ps.setLong(2, rs.getLong(2));
            ps.executeUpdate();
        }

        rs = s1.executeQuery("SELECT * FROM groups;");
        ps = db2.prepareStatement("INSERT INTO groups VALUES (?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt(1));
            ps.setNString(2, rs.getNString(2));
            ps.executeUpdate();
        }

        rs = s1.executeQuery("SELECT * FROM media;");
        ps = db2.prepareStatement("INSERT INTO media VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt("id"));
            ps.setObject(2, rs.getObject("gid"));
            ps.setNString(3, rs.getNString("path"));
            ps.setNString(4, rs.getNString("md5"));
            ps.setBinaryStream(5, rs.getBinaryStream("thumbnail"));
            ps.setBinaryStream(6, rs.getBinaryStream("hist_a"));
            ps.setBinaryStream(7, rs.getBinaryStream("hist_r"));
            ps.setBinaryStream(8, rs.getBinaryStream("hist_g"));
            ps.setBinaryStream(9, rs.getBinaryStream("hist_b"));
            //            ps.setBinaryStream(5, null);
            //            ps.setBinaryStream(6, null);
            //            ps.setBinaryStream(7, null);
            //            ps.setBinaryStream(8, null);
            //            ps.setBinaryStream(9, null);
            ps.setInt(10, rs.getInt("page"));
            ps.setBoolean(11, rs.getBoolean("no_similar"));
            ps.executeUpdate();
        }

        rs = s1.executeQuery("SELECT * FROM tagged;");
        ps = db2.prepareStatement("INSERT INTO tagged VALUES (?, ?);");
        while (rs.next()) {
            ps.setInt(1, rs.getInt(1));
            ps.setInt(2, rs.getInt(2));
            ps.executeUpdate();
        }


        s1.executeUpdate("SHUTDOWN DEFRAG;");
        s2.executeUpdate("SHUTDOWN DEFRAG;");
    }

}
