/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model.menagerie.importer;

import menagerie.gui.Main;
import menagerie.gui.screens.settings.MenagerieSettings;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.listeners.ObjectListener;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread that cleanly serializes Menagerie imports as jobs with additional features.
 */
public class ImporterThread extends Thread {

    private volatile boolean running = false;
    private volatile boolean paused = false;

    private final Menagerie menagerie;
    private final MenagerieSettings settings;
    private final Queue<ImportJob> queue = new ConcurrentLinkedQueue<>();

    private final Set<ObjectListener<ImportJob>> importerListeners = new HashSet<>();


    public ImporterThread(Menagerie menagerie, MenagerieSettings settings) {
        super("Menagerie Importer Thread");

        this.menagerie = menagerie;
        this.settings = settings;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            while (paused) {
                if (!running) return;
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            if (queue.isEmpty()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignore) {
                    }
                }
                continue;
            }

            ImportJob job = queue.remove();

            String source;
            if (job.getUrl() != null) source = job.getUrl().toString();
            else source = job.getFile().toString();
            Main.log.info(String.format("Importing: %s", source));

            job.runJob(menagerie, settings);
        }
    }

    /**
     * Adds a job to the back of the queue. FIFO.
     *
     * @param job Job to add.
     */
    public synchronized void addJob(ImportJob job) {
        job.setImporter(this);
        queue.add(job);
        notify();
        synchronized (importerListeners) {
            importerListeners.forEach(listener -> listener.pass(job));
        }
    }

    /**
     * Tells this thread to stop running. Does not forcibly stop the thread.
     */
    public synchronized void stopRunning() {
        this.running = false;
        notify();
    }

    /**
     * Sets the paused state of this import thread. If a job is already running, the paused state is not queried by this thread until the job finishes.
     *
     * @param paused Value
     */
    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        notify();
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
        queue.remove(job);
    }

}
