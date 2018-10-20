package menagerie.model.db;

import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdateQueue implements Runnable {

    private List<Runnable> activeQueue = new ArrayList<>();
    private List<Runnable> waitingQueue = new ArrayList<>();


    public synchronized void enqueueUpdate(Runnable runnable) {
        waitingQueue.add(runnable);
    }

    private synchronized boolean isActiveQueueEmpty() {
        return activeQueue.isEmpty();
    }

    private synchronized boolean isWaitingQueueEmpty() {
        return waitingQueue.isEmpty();
    }

    private synchronized Runnable dequeueUpdate() {
        if (activeQueue.isEmpty()) return null;
        return activeQueue.remove(0);
    }

    public synchronized void commit() {
        if (!isWaitingQueueEmpty()) {
            activeQueue.addAll(waitingQueue);
            waitingQueue.clear();
            notify();
        }
    }

    public synchronized int getQueueSize() {
        return activeQueue.size();
    }

    @Override
    public void run() {
        while (true) {
            long t = System.currentTimeMillis();
            System.out.println(Thread.currentThread() + " - Starting queue: " + getQueueSize() + " queued");
            while (!isActiveQueueEmpty()) {
                Runnable r = dequeueUpdate();
                r.run();
            }
            System.out.println(Thread.currentThread() + " - Queue emptied after " + (System.currentTimeMillis() - t) / 1000.0 + "s");

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
