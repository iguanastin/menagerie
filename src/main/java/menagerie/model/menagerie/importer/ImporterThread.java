package menagerie.model.menagerie.importer;

import menagerie.gui.Main;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.Settings;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class ImporterThread extends Thread {

    private volatile boolean running = true;
    private volatile boolean paused = false;

    private final Menagerie menagerie;
    private final Settings settings;
    private final BlockingQueue<ImportJob> jobs = new LinkedBlockingQueue<>();

    private final Set<ImporterJobListener> importerListeners = new HashSet<>();


    public ImporterThread(Menagerie menagerie, Settings settings) {
        this.menagerie = menagerie;
        this.settings = settings;
    }

    @Override
    public void run() {
        while (running) {
            try {
                ImportJob job = jobs.take();

                while (paused) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException ignore) {
                        }
                    }
                    if (!running) break;
                }

                if (running) job.runJob(menagerie, settings);
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
