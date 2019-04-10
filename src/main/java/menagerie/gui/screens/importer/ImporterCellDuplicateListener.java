package menagerie.gui.screens.importer;

import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;

import java.util.List;

public interface ImporterCellDuplicateListener {

    void resolveDuplicates(List<SimilarPair<MediaItem>> pairs);

}
