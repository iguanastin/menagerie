package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public abstract class SearchRule implements Comparable<SearchRule> {

    int priority = Integer.MAX_VALUE;

    public abstract boolean accept(ImageInfo img);

    @Override
    public int compareTo(SearchRule o) {
        return priority - o.priority;
    }

}
