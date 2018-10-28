package menagerie.model.db;

import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdateQueue implements Runnable {

    private List<Runnable> activeQueue = new ArrayList<>();
    private List<Runnable> waitingQueue = new ArrayList<>();

    private static final long DELTA_SINCE_LAST_PRINT = 30000;
    private long lastPrintTime = System.currentTimeMillis();
    private int jobsSinceLastPrint = 0;


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
            while (!isActiveQueueEmpty()) {
                Runnable r = dequeueUpdate();
                try {
                    r.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                jobsSinceLastPrint++;
            }

            if (System.currentTimeMillis() - lastPrintTime > DELTA_SINCE_LAST_PRINT) {
                System.out.println(Thread.currentThread() + " - Finished " + jobsSinceLastPrint + " jobs over the last " + (System.currentTimeMillis() - lastPrintTime) / 1000.0 + "s");
                lastPrintTime = System.currentTimeMillis();
                jobsSinceLastPrint = 0;
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

}
