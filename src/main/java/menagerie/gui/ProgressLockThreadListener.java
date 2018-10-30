package menagerie.gui;

public interface ProgressLockThreadListener {

    void progressUpdated(int num, int total, boolean finished);

}
