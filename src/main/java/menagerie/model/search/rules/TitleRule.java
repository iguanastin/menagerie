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

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rule that checks if the item's file path contains a string.
 */
public class TitleRule extends SearchRule {

  private final List<String> words;

  /**
   * @param text     String to find in item's file path. Case in-sensitive.
   * @param inverted Negate the rule.
   */
  public TitleRule(String text, boolean inverted) {
    super(inverted);
    this.words = new ArrayList<>(Arrays.asList(text.toLowerCase().split("\\s+")));
  }

  /**
   * Accept item if its title contains all keywords.
   * @param item Item to check.
   * @return True, if title contains all keywords.
   */
  @Override
  protected boolean checkRule(Item item) {
    if (item instanceof GroupItem) {
      final String title = ((GroupItem) item).getTitle().toLowerCase();

      for (String word : words) {
        if (!title.contains(word)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    String result = "Group Title Rule: \"" + words + "\"";
    if (isInverted()) {
      result += " [inverted]";
    }
    return result;
  }

}
