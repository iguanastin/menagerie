package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.search.rules.SearchRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Search {

    private final Menagerie menagerie;
    private SearchUpdateListener listener = null;

    private final List<SearchRule> rules;
    private final boolean descending;

    private final Comparator<ImageInfo> comparator;

    private final List<ImageInfo> results = new ArrayList<>();


    public Search(Menagerie menagerie, List<SearchRule> rules, boolean descending) {
        if (rules == null) rules = new ArrayList<>();
        this.menagerie = menagerie;
        this.rules = rules;
        this.descending = descending;
        rules.sort(null);

        comparator = (o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        };

        menagerie.registerSearch(this);

        addIfValid(menagerie.getImages());
    }

    public void setListener(SearchUpdateListener listener) {
        this.listener = listener;
    }

    public List<ImageInfo> getResults() {
        return results;
    }

    public Comparator<ImageInfo> getComparator() {
        return comparator;
    }

    public void addIfValid(List<ImageInfo> images) {
        if (rules == null) return;

        List<ImageInfo> toAdd = new ArrayList<>(images);

        for (ImageInfo img : images) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) {
                    toAdd.remove(img);
                    break;
                }
            }
        }

        boolean changed = results.addAll(toAdd);
        results.sort(getComparator());

        if (changed && listener != null) listener.imagesAdded(toAdd);
    }

    public void removeIfInvalid(List<ImageInfo> images) {
        if (rules == null) return;

        List<ImageInfo> toRemove = new ArrayList<>();

        for (ImageInfo img : images) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) {
                    toRemove.add(img);
                    break;
                }
            }
        }

        if (results.removeAll(toRemove) && listener != null) listener.imagesRemoved(toRemove);
    }

    public void remove(List<ImageInfo> images) {
        if (results.removeAll(images) && listener != null) listener.imagesRemoved(images);
    }

    public void close() {
        menagerie.closeSearch(this);
        results.clear();
        listener = null;
    }

}
