package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

import java.util.List;

public interface SearchUpdateListener {

    void imagesAdded(List<ImageInfo> img);

    void imagesRemoved(List<ImageInfo> img);

}
