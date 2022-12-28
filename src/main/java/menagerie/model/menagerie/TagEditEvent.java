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

package menagerie.model.menagerie;

import java.util.List;
import java.util.Map;

/**
 * Records an event where a set of items had tags added or removed from them.
 */
public class TagEditEvent {

  private final Map<Item, List<Tag>> added;
  private final Map<Item, List<Tag>> removed;

  /**
   * @param added   Map of Items and the Tags that were added to each of them.
   * @param removed Map of Items and the Tags that were removed from each of them.
   */
  public TagEditEvent(Map<Item, List<Tag>> added, Map<Item, List<Tag>> removed) {
    this.added = added;
    this.removed = removed;
  }

  /**
   * Undo this event by adding back tags that were removed and removing tags that were added.
   * Item's tags should be as they were before the event that created this record occurred.
   */
  public void revertAction() {
    for (Map.Entry<Item, List<Tag>> entry : added.entrySet()) {
      entry.getValue().forEach(entry.getKey()::removeTag);
    }
    for (Map.Entry<Item, List<Tag>> entry : removed.entrySet()) {
      entry.getValue().forEach(entry.getKey()::addTag);
    }
  }

  /**
   * @return The Map of Items and Tags that were added.
   */
  public Map<Item, List<Tag>> getAdded() {
    return added;
  }

  /**
   * @return The map of Items and Tags that were removed.
   */
  public Map<Item, List<Tag>> getRemoved() {
    return removed;
  }

}
