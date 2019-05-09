/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import menagerie.gui.Main;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.search.rules.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Data class that contains results of a search filtered and sorted by the given rules.
 */
public class Search {

    private final List<SearchRule> rules = new ArrayList<>();
    private final boolean showGrouped;
    private final boolean descending;
    private final String searchString;

    private final ObservableList<Item> results = FXCollections.observableArrayList();

    protected Comparator<Item> comparator;


    /**
     * Constructs a search with given rules.
     *
     * @param search         User input string to parse.
     * @param descending  Sort the results descending.
     * @param showGrouped Show items that are part of a group.
     */
    public Search(String search, boolean descending, boolean showGrouped) {
        this.descending = descending;
        this.showGrouped = showGrouped;
        this.searchString = search;

        if (search != null && !search.isEmpty()) parseRules(search);
        rules.sort(null);

        comparator = (o1, o2) -> {
            if (descending) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        };
    }

    protected void parseRules(String search) {
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
            } else if (arg.startsWith("type:") || arg.startsWith("is:")) {
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
            } else {
                rules.add(new TagRule(arg, inverted));
            }
        }
    }

    /**
     * @return List of all results currently in the search. Is a direct reference to the backing list.
     */
    public ObservableList<Item> getResults() {
        return results;
    }

    public String getSearchString() {
        return searchString;
    }

    public boolean isDescending() {
        return descending;
    }

    public boolean isShowGrouped() {
        return showGrouped;
    }

    /**
     * @return The comparator being used to sort search results.
     */
    public Comparator<Item> getComparator() {
        return comparator;
    }

    /**
     * Checks items to see if they need to be removed from or added to this search.
     *
     * @param check Items to check.
     */
    public void refreshSearch(List<Item> check) {
        List<Item> toRemove = new ArrayList<>();
        List<Item> toAdd = new ArrayList<>();
        for (Item item : check) {
            if (isItemValid(item)) {
                if (!results.contains(item)) {
                    toAdd.add(item);
                }
            } else {
                toRemove.add(item);
            }
        }

        sort();

        results.removeAll(toRemove);
        results.addAll(toAdd);
    }

    protected boolean isItemValid(Item item) {
        if (item.isInvalidated()) return false;

        if (item instanceof MediaItem && ((MediaItem) item).getGroup() != null && !showGrouped) {
            return false;
        } else {
            for (SearchRule rule : rules) {
                if (!rule.accept(item)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void sort() {
        results.sort(getComparator());
    }

}
