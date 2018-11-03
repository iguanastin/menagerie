package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public abstract class SearchRule implements Comparable<SearchRule> {

    protected int priorty = Integer.MAX_VALUE;

    public abstract boolean accept(ImageInfo img);

    @Override
    public int compareTo(SearchRule o) {
        return priorty - o.priorty;
    }

}
