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
