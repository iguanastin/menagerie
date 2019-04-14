package menagerie.model.search;

import menagerie.gui.Main;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.search.rules.*;
import menagerie.util.listeners.ObjectListener;

import java.util.*;

/**
 * Data class that contains results of a search filtered and sorted by the given rules.
 */
public class Search {

    private final List<SearchRule> rules = new ArrayList<>();
    private final boolean showGrouped;

    private final Comparator<Item> comparator;

    private final List<Item> results = new ArrayList<>();

    private Set<ObjectListener<List<Item>>> itemsAddedListeners = new HashSet<>();
    private Set<ObjectListener<List<Item>>> itemsRemovedListeners = new HashSet<>();


    /**
     * Constructs a search with given rules.
     *
     * @param search         User input string to parse.
     * @param descending  Sort the results descending.
     * @param showGrouped Show items that are part of a group.
     */
    public Search(String search, boolean descending, boolean showGrouped) {
        if (search != null && !search.isEmpty()) parseRules(search);

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

    private void parseRules(String search) {
        for (String arg : search.split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            boolean inverted = false;
            if (arg.charAt(0) == '-') {
                inverted = true;
                arg = arg.substring(1);
            }

            if (arg.startsWith("id:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                IDRule.Type type = IDRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = IDRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = IDRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new IDRule(type, Integer.parseInt(temp), inverted));
                } catch (NumberFormatException e) {
                    Main.log.warning("Failed to convert parameter to integer: " + temp);
                }
            } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = DateAddedRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = DateAddedRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new DateAddedRule(type, Long.parseLong(temp), inverted));
                } catch (NumberFormatException e) {
                    Main.log.warning("Failed to convert parameter to long: " + temp);
                }
            } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
                rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1), inverted));
            } else if (arg.startsWith("missing:")) {
                String type = arg.substring(arg.indexOf(':') + 1);
                switch (type.toLowerCase()) {
                    case "md5":
                        rules.add(new MissingRule(MissingRule.Type.MD5, inverted));
                        break;
                    case "file":
                        rules.add(new MissingRule(MissingRule.Type.FILE, inverted));
                        break;
                    case "histogram":
                    case "hist":
                        rules.add(new MissingRule(MissingRule.Type.HISTOGRAM, inverted));
                        break;
                    default:
                        Main.log.warning("Unknown type for missing type: " + type);
                        break;
                }
            } else if (arg.startsWith("type:")) {
                String type = arg.substring(arg.indexOf(':') + 1);
                if (type.equalsIgnoreCase("group")) {
                    rules.add(new TypeRule(TypeRule.Type.GROUP, inverted));
                } else if (type.equalsIgnoreCase("media")) {
                    rules.add(new TypeRule(TypeRule.Type.MEDIA, inverted));
                }
            } else if (arg.startsWith("tags:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                TagCountRule.Type type = TagCountRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = TagCountRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = TagCountRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new TagCountRule(type, Integer.parseInt(temp), inverted));
                } catch (NumberFormatException e) {
                    Main.log.warning("Failed to convert parameter to integer: " + temp);
                }
            } else if (arg.startsWith("in:")) {
                try {
                    rules.add(new InGroupRule(Integer.parseInt(arg.substring(arg.indexOf(':') + 1)), inverted));
                } catch (NumberFormatException e) {
                    Main.log.warning("Failed to convert parameter to integer: " + arg.substring(arg.indexOf(':') + 1));
                }
            } else {
                rules.add(new TagRule(arg, inverted));
            }
        }
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
