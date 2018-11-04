package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;

import java.util.ArrayList;
import java.util.List;

public class Search {

    private Menagerie menagerie;
    private SearchUpdateListener listener = null;

    private List<SearchRule> rules;
    private boolean descending;

    private List<ImageInfo> results = new ArrayList<>();


    public Search(Menagerie menagerie, List<SearchRule> rules, boolean descending) {
        this.menagerie = menagerie;
        this.rules = rules;
        this.descending = descending;
        rules.sort(null);

        menagerie.registerSearch(this);

        for (ImageInfo img : menagerie.getImages()) {
            addIfValid(img);
        }

        sortResults();
    }

    public void setListener(SearchUpdateListener listener) {
        this.listener = listener;
    }

    public List<ImageInfo> getResults() {
        return results;
    }

    public void addIfValid(ImageInfo img) {
        if (rules != null) {
            for (SearchRule rule : rules) {
                if (!rule.accept(img)) return;
            }
        }
        results.add(img);
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

    public void sortResults() {
        results.sort((o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        });
    }

    public void close() {
        menagerie.closeSearch(this);
        rules = null;
        results = null;
        listener = null;
    }

}
