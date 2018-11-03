package menagerie.gui.grid;

import menagerie.gui.progress.ProgressLockThreadCancelListener;
import menagerie.gui.progress.ProgressLockThreadFinishListener;

import java.util.List;

public interface ProgressQueueListener {

    void processProgressQueue(String title, String message, List<Runnable> queue, ProgressLockThreadFinishListener finishListener, ProgressLockThreadCancelListener cancelListener);

}
