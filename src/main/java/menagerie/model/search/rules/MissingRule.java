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
import menagerie.model.menagerie.MediaItem;

/**
 * Rule that searches for missing attributes.
 */
public class MissingRule extends SearchRule {

    public enum Type {
        MD5, FILE, HISTOGRAM
    }

    private final Type type;


    /**
     * @param type     Type of missing attribute.
     * @param inverted Negate this rule.
     */
    public MissingRule(Type type, boolean inverted) {
        super(inverted);
        this.type = type;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        if (item instanceof MediaItem) {
            result = switch (type) {
                case MD5 -> ((MediaItem) item).getMD5() == null;
                case FILE -> ((MediaItem) item).getFile() == null || !((MediaItem) item).getFile().exists();
                case HISTOGRAM -> ((MediaItem) item).getHistogram() == null;
            };
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "Missing Rule: " + type;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
