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
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.search.rules.SearchRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Data class that contains results of a search filtered and sorted by the given rules.
 */
public class Search {

  List<SearchRule> rules = new ArrayList<>();
  private final boolean showGrouped;
  private final boolean descending;
  private final boolean shuffled;
  private final String searchString;

  private final ObservableList<Item> results = FXCollections.observableArrayList();

  protected Comparator<Item> comparator;

  /**
   * Constructs a search with given rules.
   *
   * @param search      User input string to parse.
   * @param descending  Sort the results descending.
   * @param showGrouped Show items that are part of a group.
   */
  public Search(String search, boolean descending, boolean showGrouped, boolean shuffled) {
    this.descending = descending;
    this.showGrouped = showGrouped;
    this.shuffled = shuffled;
    this.searchString = search;

    if (search != null && !search.isEmpty()) {
      rules = SearchRuleParser.parseRules(search);
    }
    rules.sort(null);

    comparator = getItemComparator(descending, shuffled);
  }

  protected Comparator<Item> getItemComparator(boolean descending, boolean shuffled) {
    return (o1, o2) -> {
      if (shuffled) {
        return 0;
      }
      if (descending) {
        return o2.getId() - o1.getId();
      } else {
        return o1.getId() - o2.getId();
      }
    };
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

  public boolean isShuffled() {
    return shuffled;
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
    if (isShuffled()) {
      toAdd.forEach(item -> results.add((int) Math.floor(Math.random() * results.size()), item));
    } else {
      results.addAll(toAdd);
    }
  }

  protected boolean isItemValid(Item item) {
    if (item.isInvalidated()) {
      return false;
    }

    if (item instanceof MediaItem && ((MediaItem) item).isInGroup() && !showGrouped) {
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
