package menagerie.gui;

import java.util.ArrayList;
import java.util.List;

public class ProgressLockThread extends Thread {

    private boolean running = false;
    private final List<Runnable> queue;
    private final ProgressLockThreadListener listener;


    public ProgressLockThread(List<Runnable> queue, ProgressLockThreadListener listener) {
        this.queue = new ArrayList<>(queue);
        this.listener = listener;
    }

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized void startRunning() { running = true; }

    public synchronized void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        startRunning();

        for (int i = 0; i < queue.size() && isRunning(); i++) {
            queue.get(i).run();

            if (listener != null) listener.progressUpdated(i + 1, queue.size(), !isRunning() || i + 1 == queue.size());
        }
    }

}
