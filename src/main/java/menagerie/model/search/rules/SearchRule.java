package menagerie.model.search.rules;

import menagerie.model.menagerie.ImageInfo;

public abstract class SearchRule implements Comparable<SearchRule> {

    int priority = Integer.MAX_VALUE;

    private final boolean inverted;


    public SearchRule(boolean inverted) {
        this.inverted = inverted;
    }

    public boolean isInverted() {
        return inverted;
    }

    public abstract boolean accept(ImageInfo img);

    @Override
    public int compareTo(SearchRule o) {
        return priority - o.priority;
    }

}
