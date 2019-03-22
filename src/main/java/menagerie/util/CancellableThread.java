package menagerie.util;

public abstract class CancellableThread extends Thread {

    protected volatile boolean running = false;

    @Override
    public synchronized void start() {
        running = true;
        super.start();
    }

    public synchronized void cancel() {
        running = false;
    }

}
