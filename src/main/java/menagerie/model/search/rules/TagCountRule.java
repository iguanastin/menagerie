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
 * Rule that compares tag counts.
 */
public class TagCountRule extends SearchRule {

    public enum Type {
        EQUAL_TO, GREATER_THAN, LESS_THAN,
    }

    private final Type type;
    private final int value;


    /**
     * @param type   Type of comparison.
     * @param value  Value to compare against.
     * @param invert Negate this rule.
     */
    public TagCountRule(Type type, int value, boolean invert) {
        super(invert);
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        switch (type) {
            case EQUAL_TO:
                result = item.getTags().size() == value;
                break;
            case LESS_THAN:
                result = item.getTags().size() < value;
                break;
            case GREATER_THAN:
                result = item.getTags().size() > value;
                break;
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "Tag Count Rule: " + type + " " + value;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
