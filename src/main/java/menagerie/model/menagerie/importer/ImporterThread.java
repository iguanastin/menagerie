package menagerie.model.menagerie.importer;

import menagerie.gui.Main;
import menagerie.model.Settings;
import menagerie.model.menagerie.Menagerie;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class ImporterThread extends Thread {

    private volatile boolean running = true;
    private volatile boolean paused = false;

    private final Menagerie menagerie;
    private final Settings settings;
    private final BlockingQueue<ImportJob> jobs = new LinkedBlockingQueue<>();

    private final Set<ImporterJobListener> importerListeners = new HashSet<>();

    private final Lock loggingLock = new ReentrantLock();
    private final Timer loggingTimer = new Timer(true);
    private int importCount = 0;
    private long lastLog = System.currentTimeMillis();


    public ImporterThread(Menagerie menagerie, Settings settings) {
        this.menagerie = menagerie;
        this.settings = settings;
    }

    @Override
    public void run() {

        loggingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    loggingLock.lock();
                    if (importCount > 0) {
                        Main.log.info(String.format("ImporterThread imported %d items in the last %.2fs", importCount, (System.currentTimeMillis() - lastLog) / 1000.0));
                        lastLog = System.currentTimeMillis();
                        importCount = 0;
                    }
                } finally {
                    loggingLock.unlock();
                }
            }
        }, 30000, 30000);

        while (running) {
            try {
                ImportJob job = jobs.take();

                while (paused) {
                    synchronized (this) {
                        try {
                            wait(10000);
                        } catch (InterruptedException ignore) {
                        }
                    }
                    if (!running) break;
                }

                if (running) {
                    job.runJob(menagerie, settings);
                    try {
                        loggingLock.lock();
                        importCount++;
                    } finally {
                        loggingLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Main.log.log(Level.WARNING, "Interrupted while importer thread taking job", e);
            }
        }
    }

    public void queue(ImportJob job) {
        jobs.add(job);
        job.setImporter(this);
        synchronized (importerListeners) {
            importerListeners.forEach(listener -> listener.jobQueued(job));
        }
    }

    public synchronized void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        notifyAll();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return running;
    }

    public void addImporterListener(ImporterJobListener listener) {
        synchronized (importerListeners) {
            importerListeners.add(listener);
        }
    }

    public void removeImporterListener(ImporterJobListener listener) {
        synchronized (importerListeners) {
            importerListeners.remove(listener);
        }
    }

    public void cancel(ImportJob job) {
        jobs.remove(job);
    }

}
