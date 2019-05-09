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
 * Rule that compares item IDs.
 */
public class IDRule extends SearchRule {

    public enum Type {
        LESS_THAN, GREATER_THAN, EQUAL_TO
    }

    private final int id;
    private final Type type;


    /**
     * @param type     Type of rule.
     * @param value    Value to compare with.
     * @param inverted Negate this rule.
     */
    public IDRule(Type type, int value, boolean inverted) {
        super(inverted);
        priority = 1;

        this.type = type;
        this.id = value;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        switch (type) {
            case LESS_THAN:
                result = item.getId() < id;
                break;
            case GREATER_THAN:
                result = item.getId() > id;
                break;
            case EQUAL_TO:
                result = item.getId() == id;
                break;
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "ID Rule: " + type + " " + id;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
