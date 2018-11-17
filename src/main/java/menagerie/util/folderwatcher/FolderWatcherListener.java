package menagerie.util.folderwatcher;

import java.io.File;

public interface FolderWatcherListener {

    void foundNewFiles(File[] file);

}
