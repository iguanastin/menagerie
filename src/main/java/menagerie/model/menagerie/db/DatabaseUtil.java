package menagerie.model.menagerie.db;

import java.io.File;

public class DatabaseUtil {

  private DatabaseUtil() {
    // util class
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
