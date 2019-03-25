package menagerie.model.search;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.search.rules.SearchRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Search {

    private final Menagerie menagerie;
    private SearchUpdateListener listener = null;

    private final List<SearchRule> rules;
    private final boolean showGrouped;

    private final Comparator<Item> comparator;

    private final List<Item> results = new ArrayList<>();


    public Search(Menagerie menagerie, List<SearchRule> rules, boolean descending, boolean showGrouped) {
        if (rules == null) rules = new ArrayList<>();
        this.menagerie = menagerie;
        this.rules = rules;
        this.showGrouped = showGrouped;
        rules.sort(null);

        comparator = (o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        };

        menagerie.registerSearch(this);

        addIfValid(menagerie.getItems());
    }

    public void setListener(SearchUpdateListener listener) {
        this.listener = listener;
    }

    public List<Item> getResults() {
        return results;
    }

    private Comparator<Item> getComparator() {
        return comparator;
    }

    public void addIfValid(List<Item> items) {
        if (rules == null) return;

        List<Item> toAdd = new ArrayList<>(items);

        for (Item item : items) {
            if (item instanceof MediaItem && ((MediaItem) item).getGroup() != null && !showGrouped) {
                toAdd.remove(item);
            } else {
                for (SearchRule rule : rules) {
                    if (!rule.accept(item)) {
                        toAdd.remove(item);
                        break;
                    }
                }
            }
        }

        boolean changed = results.addAll(toAdd);
        results.sort(getComparator());

        if (changed && listener != null) listener.imagesAdded(toAdd);
    }

    public void recheckWithSearch(List<Item> items) {
        if (rules == null) return;

        List<Item> toRemove = new ArrayList<>();
        List<Item> toAdd = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof MediaItem && ((MediaItem) item).getGroup() != null && !showGrouped) {
                toRemove.add(item);
            } else {
                for (SearchRule rule : rules) {
                    if (!rule.accept(item)) {
                        toRemove.add(item);
                        break;
                    }
                }
            }

            if (!results.contains(item) && !toRemove.contains(item)) {
                toAdd.add(item);
            }
        }
        if (results.removeAll(toRemove) && listener != null) listener.imagesRemoved(toRemove);
        if (results.addAll(toAdd) && listener != null) listener.imagesAdded(toAdd);
    }

    public void remove(List<Item> images) {
        if (results.removeAll(images) && listener != null) listener.imagesRemoved(images);
    }

    public void close() {
        menagerie.closeSearch(this);
        results.clear();
        listener = null;
    }

}
