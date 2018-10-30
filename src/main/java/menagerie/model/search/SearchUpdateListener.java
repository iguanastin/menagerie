package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public interface SearchUpdateListener {

    void imageAdded(ImageInfo img);

    void imageRemoved(ImageInfo img);

}
