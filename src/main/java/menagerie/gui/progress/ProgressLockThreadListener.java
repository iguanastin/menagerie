package menagerie.gui.progress;

public interface ProgressLockThreadListener {

    void progressUpdated(int num, int total, boolean finished);

}
