package menagerie.gui;

import java.io.File;

public interface FolderWatcherListener {

    void foundNewFiles(File[] file);

}
