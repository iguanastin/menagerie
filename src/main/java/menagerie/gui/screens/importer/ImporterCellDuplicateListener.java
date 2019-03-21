package menagerie.gui.screens.importer;

import menagerie.model.SimilarPair;

import java.util.List;

public interface ImporterCellDuplicateListener {

    void resolveDuplicates(List<SimilarPair> pairs);

}
