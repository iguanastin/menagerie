/*
 MIT License

 Copyright (c) 2020. Austin Thompson

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
import menagerie.model.menagerie.MediaItem;

/**
 * Rule that requires item be in a specific group.
 */
public class InGroupRule extends SearchRule {

    private final int id;


    /**
     * @param value    Value to compare with.
     * @param inverted Negate this rule.
     */
    public InGroupRule(int value, boolean inverted) {
        super(inverted);
        priority = 10;

        this.id = value;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = item instanceof MediaItem && ((MediaItem) item).isInGroup() && ((MediaItem) item).getGroup().getId() == id;

        return isInverted() != result;
    }

    @Override
    public String toString() {
        String result = "In Group Rule: " + id;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
