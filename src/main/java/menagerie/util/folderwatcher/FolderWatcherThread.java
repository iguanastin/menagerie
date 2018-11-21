package menagerie.util.folderwatcher;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;

public class FolderWatcherThread extends Thread {

    private boolean running = false;

    private final long timeout;
    private final File watchFolder;
    private final FileFilter filter;

    private final FolderWatcherListener listener;

    public FolderWatcherThread(File watchFolder, FileFilter filter, long timeout, FolderWatcherListener listener) {
        this.watchFolder = watchFolder;
        this.filter = filter;
        this.timeout = timeout;
        this.listener = listener;
    }

    private synchronized boolean isRunning() {
        return running;
    }

    public synchronized void stopWatching() {
        running = false;
        notifyAll();
    }

    @Override
    public void run() {
        if (listener == null || watchFolder == null) return;

        running = true;

        while (isRunning()) {
            File folder = watchFolder;
            if (folder != null && folder.exists() && folder.isDirectory()) {
                File[] files = Objects.requireNonNull(folder.listFiles(filter));
                if (files.length > 0) listener.foundNewFiles(files);
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
