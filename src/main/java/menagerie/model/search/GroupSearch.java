package menagerie.model.search;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class GroupSearch extends Search {

    private final GroupItem group;


    /**
     * Constructs a group search with given rules.
     *
     * @param search     User input string to parse.
     * @param group      Group scope of this search.
     * @param descending Sort the results descending.
     */
    public GroupSearch(String search, GroupItem group, boolean descending) {
        super(search, descending, true);
        this.group = group;

        comparator = (o1, o2) -> {
            if (o1 instanceof MediaItem && o2 instanceof MediaItem) {
                if (descending) {
                    return ((MediaItem) o2).getPageIndex() - ((MediaItem) o1).getPageIndex();
                } else {
                    return ((MediaItem) o1).getPageIndex() - ((MediaItem) o2).getPageIndex();
                }
            } else {
                return 0;
            }
        };
    }

    @Override
    protected boolean isItemValid(Item item) {
        if (item instanceof MediaItem) {
            if (!((MediaItem) item).inGroup() || !((MediaItem) item).getGroup().equals(group)) {
                return false;
            }
        } else {
            return false;
        }

        return super.isItemValid(item);
    }

    public GroupItem getGroup() {
        return group;
    }

}
