package menagerie.util.folderwatcher;

import java.io.File;
import java.io.FileFilter;

public class FolderWatcherThread extends Thread {

    private volatile boolean running = false;

    private final long timeout;
    private final File watchFolder;
    private final FileFilter filter;

    private final FolderWatcherListener listener;

    /**
     * Constructs a folder watcher thread.
     *
     * @param watchFolder Target folder to watch for files.
     * @param filter      Filter to reduce results.
     * @param timeout     Time between file checks.
     * @param listener    Listener to notify when files are found.
     */
    public FolderWatcherThread(File watchFolder, FileFilter filter, long timeout, FolderWatcherListener listener) {
        this.watchFolder = watchFolder;
        this.filter = filter;
        this.timeout = timeout;
        this.listener = listener;
    }

    /**
     * Tell thread to stop watching for files. Does not forcibly stop the thread.
     */
    public void stopWatching() {
        running = false;
        notify();
    }

    @Override
    public void run() {
        if (listener == null || watchFolder == null) return;

        running = true;

        while (running) {
            File folder = watchFolder;
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles(filter);
                if (files != null && files.length > 0) listener.foundNewFiles(files);
            }

            try {
                synchronized (this) {
                    wait(timeout);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

}
