package menagerie.model.search;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.search.rules.InGroupRule;
import menagerie.model.search.rules.SearchRule;
import menagerie.util.listeners.ObjectListener;

import java.util.*;

/**
 * Data class that contains results of a search filtered and sorted by the given rules.
 */
public class Search {

    private final List<SearchRule> rules;
    private final boolean showGrouped;

    private final Comparator<Item> comparator;

    private final List<Item> results = new ArrayList<>();

    private Set<ObjectListener<List<Item>>> itemsAddedListeners = new HashSet<>();
    private Set<ObjectListener<List<Item>>> itemsRemovedListeners = new HashSet<>();


    /**
     * Constructs a search with given rules.
     *
     * @param rules       Item rules.
     * @param descending  Sort the results descending.
     * @param showGrouped Show items that are part of a group.
     */
    public Search(List<SearchRule> rules, boolean descending, boolean showGrouped) {
        if (rules == null) rules = new ArrayList<>();
        this.rules = rules;
        rules.sort(null);

        // Show grouped items if the search contains an InGroupRule
        if (!showGrouped) {
            for (SearchRule rule : rules) {
                if (rule instanceof InGroupRule) {
                    showGrouped = true;
                    break;
                }
            }
        }
        this.showGrouped = showGrouped;

        comparator = (o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        };
    }

    /**
     * Adds a listener that listens for items being added.
     *
     * @param listener Listener to add.
     * @return True if successful.
     */
    public boolean addItemsAddedListener(ObjectListener<List<Item>> listener) {
        return itemsAddedListeners.add(listener);
    }

    /**
     * Adds a listener that listens for items being removed.
     *
     * @param listener Listener to add.
     * @return True if successful.
     */
    public boolean addItemsRemovedListener(ObjectListener<List<Item>> listener) {
        return itemsRemovedListeners.add(listener);
    }

    /**
     * Removes a listener that listens for items being added.
     *
     * @param listener Listener to remove.
     * @return True if successful.
     */
    public boolean removeItemsAddedListener(ObjectListener<List<Item>> listener) {
        return itemsAddedListeners.remove(listener);
    }

    /**
     * Removes a listener that listens for items being removed.
     *
     * @param listener Listener to remove.
     * @return True if successful.
     */
    public boolean removeItemsRemovedListener(ObjectListener<List<Item>> listener) {
        return itemsRemovedListeners.remove(listener);
    }

    /**
     * @return List of all results currently in the search. Is a direct reference to the backing list.
     */
    public List<Item> getResults() {
        return results;
    }

    /**
     * @return The comparator being used to sort search results.
     */
    public Comparator<Item> getComparator() {
        return comparator;
    }

    /**
     * Adds all the accepted items in the given list to this search. Ignores items that do not fit the rules of this search.
     *
     * @param items Items to attempt to add.
     */
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

        if (changed) itemsAddedListeners.forEach(listener -> listener.pass(toAdd));
    }

    /**
     * Checks items to see if they need to be removed from or added to this search.
     *
     * @param items Items to check.
     */
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
        if (results.removeAll(toRemove)) itemsRemovedListeners.forEach(listener -> listener.pass(toRemove));
        if (results.addAll(toAdd)) itemsAddedListeners.forEach(listener -> listener.pass(toAdd));
    }

    /**
     * Removes items from this search.
     *
     * @param items Items to remove.
     */
    public void remove(List<Item> items) {
        if (results.removeAll(items)) itemsRemovedListeners.forEach(listener -> listener.pass(items));
    }

}
