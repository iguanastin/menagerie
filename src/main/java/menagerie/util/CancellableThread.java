package menagerie.util;

/**
 * Simple thread template that can be cancelled mid-run.
 */
public abstract class CancellableThread extends Thread {

    protected volatile boolean running = false;

    @Override
    public synchronized void start() {
        running = true;
        super.start();
    }

    /**
     * Tells the thread to stop running. Does not forcibly stop the thread.
     */
    public synchronized void cancel() {
        running = false;
    }

}
