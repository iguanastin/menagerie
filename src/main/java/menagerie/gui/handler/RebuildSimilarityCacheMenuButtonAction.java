package menagerie.gui.handler;

import javafx.application.Platform;
import menagerie.gui.screens.dialogs.ProgressScreen;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.CancellableThread;

public class RebuildSimilarityCacheMenuButtonAction extends CancellableThread {

  private final Menagerie menagerie;
  private final ProgressScreen ps;

  public RebuildSimilarityCacheMenuButtonAction(Menagerie menagerie, ProgressScreen ps) {
    this.menagerie = menagerie;
    this.ps = ps;
  }

  // REENG: reduce complexity
  @Override
  public void run() {
    final int total = menagerie.getItems().size();
    final double confidenceSquare =
        1 - (1 - MediaItem.MIN_CONFIDENCE) * (1 - MediaItem.MIN_CONFIDENCE);

    for (int i = 0; i < menagerie.getItems().size(); i++) {
      if (!(menagerie.getItems().get(i) instanceof MediaItem)) {
        continue;
      }
      MediaItem i1 = (MediaItem) menagerie.getItems().get(i);
      if (i1.getHistogram() == null) {
        continue;
      }

      boolean hasSimilar = false;
      for (int j = 0; j < menagerie.getItems().size(); j++) {
        if (i == j) {
          continue;
        }
        if (!(menagerie.getItems().get(j) instanceof MediaItem)) {
          continue;
        }
        MediaItem i2 = (MediaItem) menagerie.getItems().get(j);
        if (i2.getHistogram() == null || i2.hasNoSimilar()) {
          continue;
        }

        double similarity = i1.getSimilarityTo(i2);
        if (similarity >= confidenceSquare ||
            ((i1.getHistogram().isColorful() || i2.getHistogram().isColorful()) &&
             similarity > MediaItem.MIN_CONFIDENCE)) {
          hasSimilar = true;
          break;
        }
      }

      i1.setHasNoSimilar(!hasSimilar);

      final int finalI = i;
      Platform.runLater(() -> ps.setProgress(finalI, total));
    }

    Platform.runLater(ps::close);
  }

}
