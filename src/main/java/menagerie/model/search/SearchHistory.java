package menagerie.model.search;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.util.ArrayList;
import java.util.List;

public class SearchHistory {

    private final List<Item> selected = new ArrayList<>();
    private final String search;
    private final boolean descending;
    private final boolean showGrouped;
    private final GroupItem groupScope;


    public SearchHistory(String search, GroupItem scope, List<Item> selected, boolean descending, boolean showGrouped) {
        this.search = search;
        this.groupScope = scope;
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

    public GroupItem getGroupScope() {
        return groupScope;
    }

    public boolean isDescending() {
        return descending;
    }

    public boolean isShowGrouped() {
        return showGrouped;
    }

}
