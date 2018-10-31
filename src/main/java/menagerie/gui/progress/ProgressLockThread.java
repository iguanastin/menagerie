package menagerie.gui.progress;

import java.util.ArrayList;
import java.util.List;

public class ProgressLockThread extends Thread {

    private boolean running = false;
    private final List<Runnable> queue;
    private ProgressLockThreadCancelListener cancelListener;
    private ProgressLockThreadFinishListener finishListener;
    private ProgressLockThreadUpdateListener updateListener;


    public ProgressLockThread(List<Runnable> queue) {
        this.queue = new ArrayList<>(queue);
    }

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized void startRunning() {
        running = true;
    }

    public synchronized void stopRunning() {
        running = false;
    }

    public void setCancelListener(ProgressLockThreadCancelListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    public void setFinishListener(ProgressLockThreadFinishListener finishListener) {
        this.finishListener = finishListener;
    }

    public void setUpdateListener(ProgressLockThreadUpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    @Override
    public void run() {
        startRunning();

        int i = 0;
        for (; i < queue.size() && isRunning(); i++) {
            queue.get(i).run();

            if (updateListener != null) updateListener.progressUpdated(i + 1, queue.size());
        }

        if (i < queue.size()) {
            if (cancelListener != null) cancelListener.progressCanceled(i, queue.size());
        } else {
            if (finishListener != null) finishListener.progressFinished(queue.size());
        }
    }

}
