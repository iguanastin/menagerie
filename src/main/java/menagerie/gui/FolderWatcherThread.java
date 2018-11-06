package menagerie.gui;

import java.io.File;
import java.io.FileFilter;
import java.util.Objects;

public class FolderWatcherThread extends Thread {

    private boolean running = false;

    private long timeout;
    private File watchFolder;
    private FileFilter filter;

    private FolderWatcherListener listener;

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
