package menagerie.gui.progress;

import java.util.List;

public interface ProgressQueueListener {

    void processProgressQueue(String title, String message, List<Runnable> queue);

}
