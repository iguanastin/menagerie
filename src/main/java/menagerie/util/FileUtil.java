package menagerie.util;

import java.io.File;

public class FileUtil {

  private FileUtil() {
    // util class
  }


  /**
   * Attempts to resolve a filename conflict caused by a pre-existing file at the same path.
   * Appends an incremented number surrounded by parenthesis to the file if it already exists.
   *
   * @param file File to resolve name for.
   * @return File pointing to a file that does not exist yet.
   */
  public static File resolveDuplicateFilename(File file) {
    while (file.exists()) {
      String name = file.getName();
      if (name.matches(".*\\s\\([0-9]+\\)\\..*")) {
        int count =
            Integer.parseInt(name.substring(name.lastIndexOf('(') + 1, name.lastIndexOf(')')));
        name = name.substring(0, name.lastIndexOf('(') + 1) + (count + 1) +
               name.substring(name.lastIndexOf(')'));
      } else {
        name = name.substring(0, name.lastIndexOf('.')) + " (2)" +
               name.substring(name.lastIndexOf('.'));
      }

      String parent = file.getParent();
      if (!parent.endsWith("/") && !parent.endsWith("\\")) {
        parent += "/";
      }
      file = new File(parent + name);
    }

    return file;
  }
}
