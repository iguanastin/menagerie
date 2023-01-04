package menagerie.model.menagerie.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import menagerie.model.menagerie.Menagerie;

public class DatabaseUtil {

  private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());

  private DatabaseUtil() {
    // util class
  }

  public static void shutDownDatabase(boolean revertDatabase, Menagerie menagerie, String dbUrl) {
    new Thread(() -> {
      try {
        LOGGER.info("Attempting to shut down Menagerie database and defragment the file");
        menagerie.getDatabaseManager().shutdownDefrag();
        LOGGER.info("Done defragging database file");

        if (revertDatabase) {
          File database = DatabaseUtil.resolveDatabaseFile(dbUrl);
          File backup = new File(database + ".bak");
          LOGGER.warning(String.format("Reverting to last backup database: %s", backup));
          try {
            Files.move(backup.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Failed to revert the database: " + database);
          }
        }

        LOGGER.info("Finished shutting down...");
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "SQL exception when shutting down with defrag", e);
      }

      System.exit(0);
    }, "Shutdown Menagerie").start();
  }

  /**
   * Attempts to resolve the actual path to the database file by java path standards, given a JDBC database path.
   *
   * @param databaseURL JDBC style path to database.
   * @return Best attempt at resolving the path.
   */
  public static File resolveDatabaseFile(String databaseURL) {
    String path = databaseURL + ".mv.db";
    if (path.startsWith("~")) {
      String temp = System.getProperty("user.home");
      if (!temp.endsWith("/") && !temp.endsWith("\\")) {
        temp += "/";
      }
      path = path.substring(1);
      if (path.startsWith("/") || path.startsWith("\\")) {
        path = path.substring(1);
      }

      path = temp + path;
    }

    return new File(path);
  }
}
