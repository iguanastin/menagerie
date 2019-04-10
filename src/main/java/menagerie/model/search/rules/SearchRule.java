package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;

/**
 * Abstract class defining a search rule.
 */
public abstract class SearchRule implements Comparable<SearchRule> {

    /**
     * Sort order priority of the rule.
     */
    int priority = Integer.MAX_VALUE;

    private final boolean inverted;


    /**
     * Constructs this rule and initializes the inverted state.
     *
     * @param inverted Logically negate this rule.
     */
    public SearchRule(boolean inverted) {
        this.inverted = inverted;
    }

    /**
     * @return True if this rule is logically inverted.
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Checks whether an item aligns with this rule.
     *
     * @param item Item to check.
     * @return True if the item is accepted by this rule.
     */
    public abstract boolean accept(Item item);

    @Override
    public int compareTo(SearchRule o) {
        return priority - o.priority;
    }

}
