package menagerie.model.menagerie.importer;

import menagerie.gui.Main;
import menagerie.model.Settings;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.listeners.ObjectListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * A thread that cleanly serializes Menagerie imports as jobs with additional features.
 */
public class ImporterThread extends Thread {

    private volatile boolean running = false;
    private volatile boolean paused = false;

    private final Menagerie menagerie;
    private final Settings settings;
    private final BlockingQueue<ImportJob> jobs = new LinkedBlockingQueue<>();

    private final Set<ObjectListener<ImportJob>> importerListeners = new HashSet<>();


    public ImporterThread(Menagerie menagerie, Settings settings) {
        this.menagerie = menagerie;
        this.settings = settings;
    }

    @Override
    public void run() {
        running = true;
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
                    String source;
                    if (job.getUrl() != null) source = job.getUrl().toString();
                    else source = job.getFile().toString();
                    Main.log.info(String.format("Importing: %s", source));

                    job.runJob(menagerie, settings);
                }
            } catch (InterruptedException e) {
                Main.log.log(Level.WARNING, "Interrupted while importer thread taking job", e);
            }
        }
    }

    /**
     * Adds a job to the back of the queue. FIFO.
     *
     * @param job Job to add.
     */
    public void addJob(ImportJob job) {
        jobs.add(job);
        job.setImporter(this);
        synchronized (importerListeners) {
            importerListeners.forEach(listener -> listener.pass(job));
        }
    }

    /**
     * Tells this thread to stop running. Does not forcibly stop the thread.
     */
    public synchronized void stopRunning() {
        this.running = false;
    }

    /**
     * Sets the paused state of this import thread. If a job is already running, the paused state is not queried by this thread until the job finishes.
     *
     * @param paused Value
     */
    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        notifyAll();
    }

    /**
     * @return True if this thread is paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @return True if this thread is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param listener Listener that listens for jobs being added.
     */
    public void addImporterListener(ObjectListener<ImportJob> listener) {
        synchronized (importerListeners) {
            importerListeners.add(listener);
        }
    }

    /**
     * @param listener Listener to remove.
     */
    public void removeImporterListener(ObjectListener<ImportJob> listener) {
        synchronized (importerListeners) {
            importerListeners.remove(listener);
        }
    }

    /**
     * Cancels a job, if it has not already been consumed/ran.
     *
     * @param job Job to remove.
     */
    public void cancel(ImportJob job) {
        jobs.remove(job);
    }

}
