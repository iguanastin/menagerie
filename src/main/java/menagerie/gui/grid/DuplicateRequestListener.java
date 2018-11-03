package menagerie.gui.grid;

import menagerie.model.menagerie.ImageInfo;

import java.util.List;

public interface DuplicateRequestListener {

    void findAndShowDuplicates(List<ImageInfo> images);

}
