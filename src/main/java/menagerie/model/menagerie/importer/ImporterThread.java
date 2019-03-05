package menagerie.model.menagerie.importer;

import menagerie.model.menagerie.Menagerie;
import menagerie.model.Settings;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ImporterThread extends Thread {

    private volatile boolean running = true;

    private final Menagerie menagerie;
    private final Settings settings;
    private final BlockingQueue<ImportJob> jobs = new LinkedBlockingQueue<>();


    public ImporterThread(Menagerie menagerie, Settings settings) {
        this.menagerie = menagerie;
        this.settings = settings;
    }

    @Override
    public void run() {
        while (running) {
            try {
                ImportJob job = jobs.take();

                job.runJob(menagerie, settings);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void queue(ImportJob job) {
        jobs.add(job);
    }

    public synchronized void setRunning(boolean running) {
        this.running = running;
    }
}
