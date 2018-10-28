package menagerie.model.search;

import menagerie.model.ImageInfo;

public interface SearchUpdateListener {

    void imageAdded(ImageInfo img);

    void imageRemoved(ImageInfo img);

}
