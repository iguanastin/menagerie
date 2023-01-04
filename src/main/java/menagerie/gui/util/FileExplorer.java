package menagerie.gui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class FileExplorer {

  private FileExplorer() {
  }

  private static final Logger LOGGER = Logger.getLogger(FileExplorer.class.getName());

  public static void openExplorer(List<Item> selected) {
    Item last = selected.get(selected.size() - 1);
    if (last instanceof MediaItem) {
      try {
        Runtime.getRuntime()
            .exec("explorer.exe /select, " + ((MediaItem) last).getFile().getAbsolutePath());
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error opening file in explorer", e);
      }
    }
  }

  public static void openDefault(List<Item> selected) {
    Item last = selected.get(selected.size() - 1);
    if (last instanceof MediaItem) {
      try {
        Desktop.getDesktop().open(((MediaItem) last).getFile());
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Error opening file with system default program", e);
      }
    }
  }

  public static boolean hasAllowedFileEnding(String name) {
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
           name.endsWith(".bmp");
  }
}
