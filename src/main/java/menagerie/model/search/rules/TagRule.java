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
import menagerie.model.menagerie.Tag;

/**
 * Rule that checks if an item has a tag.
 */
public class TagRule extends SearchRule {

  private final String tag;

  /**
   * @param tag     Tag to find.
   * @param exclude Negate this rule.
   */
  public TagRule(String tag, boolean exclude) {
    super(exclude);
    priority = 25;

    this.tag = tag;
  }

  @Override
  protected boolean checkRule(Item item) {
    for (Tag t : item.getTags()) {
      if (t.getName().equalsIgnoreCase(tag)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String result = "Tag Rule: \"" + tag + "\"";
    if (isInverted()) {
      result += " [inverted]";
    }
    return result;
  }

}
