package menagerie.util;

import java.util.ArrayList;
import java.util.List;

public class RunnableQueue implements Runnable {

    private List<Runnable> queue = new ArrayList<>();


    public synchronized void enqueueUpdate(Runnable runnable) {
        queue.add(runnable);
        notify();
    }

    private synchronized boolean isQueueEmpty() {
        return queue.isEmpty();
    }

    private synchronized Runnable dequeueUpdate() {
        if (queue.isEmpty()) return null;
        return queue.remove(0);
    }

    @Override
    public void run() {
        while (true) {
            while (!isQueueEmpty()) {
                Runnable r = dequeueUpdate();
                r.run();
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
