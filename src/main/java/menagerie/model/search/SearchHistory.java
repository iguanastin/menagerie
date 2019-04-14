package menagerie.model.search;

import menagerie.model.menagerie.Item;

import java.util.ArrayList;
import java.util.List;

public class SearchHistory {

    private final List<Item> selected = new ArrayList<>();
    private final String search;
    private final boolean descending;
    private final boolean showGrouped;


    public SearchHistory(String search, List<Item> selected, boolean descending, boolean showGrouped) {
        this.search = search;
        this.descending = descending;
        this.showGrouped = showGrouped;
        this.selected.addAll(selected);
    }

    public List<Item> getSelected() {
        return selected;
    }

    public String getSearch() {
        return search;
    }

    public boolean isDescending() {
        return descending;
    }

    public boolean isShowGrouped() {
        return showGrouped;
    }

}
