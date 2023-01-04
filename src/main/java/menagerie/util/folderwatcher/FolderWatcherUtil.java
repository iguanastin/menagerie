package menagerie.util.folderwatcher;

import java.io.File;
import java.util.logging.Logger;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.util.FileUtil;

public class FolderWatcherUtil {

  private static final Logger LOGGER = Logger.getLogger(FolderWatcherUtil.class.getName());

  private FolderWatcherUtil() {
  }

  public static void folderWatchListener(boolean moveToDefault, File[] files,
                                         String defaultFolder, ImporterThread importer) {

    for (File file : files) {
      LOGGER.info("Folder watcher got file to import: " + file);
      if (moveToDefault) {
        String work = defaultFolder;
        if (!work.endsWith("/") && !work.endsWith("\\")) {
          work += "/";
        }
        File f = new File(work + file.getName());
        if (file.equals(f)) {
          continue; //File is being "moved" to same folder
        }

        File dest = FileUtil.resolveDuplicateFilename(f);

        if (!file.renameTo(dest)) {
          continue;
        }

        file = dest;
      }

      importer.addJob(new ImportJob(file, null));
    }
  }

}
