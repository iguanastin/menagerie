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

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.util.ArrayList;
import java.util.List;

public class SearchHistory {

    private final List<Item> selected = new ArrayList<>();
    private final String search;
    private final boolean descending;
    private final boolean showGrouped;
    private final boolean shuffled;
    private final GroupItem groupScope;


    public SearchHistory(String search, GroupItem scope, List<Item> selected, boolean descending, boolean showGrouped, boolean shuffled) {
        this.search = search;
        this.groupScope = scope;
        this.descending = descending;
        this.showGrouped = showGrouped;
        this.shuffled = shuffled;
        this.selected.addAll(selected);
    }

    public List<Item> getSelected() {
        return selected;
    }

    public String getSearch() {
        return search;
    }

    public GroupItem getGroupScope() {
        return groupScope;
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

}
