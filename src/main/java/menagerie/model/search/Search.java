package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;

import java.util.ArrayList;
import java.util.Collections;
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

        for (ImageInfo img : menagerie.getImages()) {
            addIfValid(img);
        }
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

    public void addIfValid(ImageInfo img) {
        if (rules != null) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) return;
            }
        }

        boolean added = false;

        for (int i = 0; i < results.size(); i++) {
            if (comparator.compare(img, results.get(i)) < 0) {
                results.add(i, img);
                added = true;
                break;
            }
        }

        if (!added) results.add(img);

        if (listener != null) listener.imageAdded(img);
    }

    public void removeIfInvalid(ImageInfo img) {
        if (rules != null) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) {
                    boolean result = results.remove(img);
                    if (listener != null && result) listener.imageRemoved(img);
                    return;
                }
            }
        }

    }

    public void remove(ImageInfo img) {
        boolean result = results.remove(img);
        if (listener != null && result) listener.imageRemoved(img);

    }

    public void close() {
        menagerie.closeSearch(this);
        results.clear();
        listener = null;
    }

}
