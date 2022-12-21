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

import java.util.Date;
import menagerie.model.menagerie.Item;

/**
 * Rule that compares against the date an item was added to the Menagerie.
 */
public class DateAddedRule extends SearchRule {

  public enum Type {
    LESS_THAN, GREATER_THAN, EQUAL_TO
  }

  private final long time;
  private final Type type;


  /**
   * @param type     Type of this rule.
   * @param time     Time to compare item to.
   * @param inverted Negate the rule.
   */
  public DateAddedRule(Type type, long time, boolean inverted) {
    super(inverted);
    priority = 10;

    this.time = time;
    this.type = type;
  }

  @Override
  public boolean accept(Item item) {
    boolean result = false;
    switch (type) {
      case LESS_THAN:
        result = item.getDateAdded() < time;
        break;
      case GREATER_THAN:
        result = item.getDateAdded() > time;
        break;
      case EQUAL_TO:
        result = item.getDateAdded() == time;
        break;
    }

    if (isInverted()) {
      result = !result;
    }

    return result;
  }

  @Override
  public String toString() {
    String result = "Added Date Rule: " + type + " " + time + " (" + new Date(time) + ")";
    if (isInverted()) {
      result += " [inverted]";
    }
    return result;
  }

    public long getTime() {
        return time;
    }

    public Type getType() {
        return type;
    }
}
