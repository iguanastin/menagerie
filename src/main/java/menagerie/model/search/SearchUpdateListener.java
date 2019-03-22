package menagerie.model.search;

import menagerie.model.menagerie.Item;

import java.util.List;

public interface SearchUpdateListener {

    void imagesAdded(List<Item> img);

    void imagesRemoved(List<Item> img);

}
