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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import menagerie.gui.Thumbnail;
import org.junit.jupiter.api.Test;

class ItemTests {

  @Test
  void createGet() {
    final int id = 12;
    final long added = System.currentTimeMillis();

    Item item = new Item(null, id, added) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };

    assertEquals(id, item.getId());
    assertEquals(added, item.getDateAdded());
    assertNotNull(item.getTags());
  }

  @Test
  void tagEditing() {
    Item item = new Item(null, 1, 1) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };

    Tag t1 = new Tag(null, 1, "tag1", null);
    Tag t2 = new Tag(null, 2, "tag2", null);
    Tag t3 = new Tag(null, 3, "tag3", null);

    assertFalse(item.hasTag(t1));
    assertFalse(item.hasTag(t2));
    assertFalse(item.hasTag(t3));

    assertTrue(item.addTag(t1));
    assertTrue(item.getTags().contains(t1));
    assertTrue(item.hasTag(t1));

    assertFalse(item.addTag(t1));
    assertTrue(item.getTags().contains(t1));
    assertTrue(item.hasTag(t1));

    assertTrue(item.addTag(t2));
    assertTrue(item.addTag(t3));
    assertTrue(item.hasTag(t2));
    assertTrue(item.hasTag(t3));
    assertTrue(item.getTags().contains(t2));
    assertTrue(item.getTags().contains(t3));

    assertTrue(item.removeTag(t1));
    assertFalse(item.hasTag(t1));
    assertFalse(item.getTags().contains(t1));
  }

  @Test
  void hash() {
    Item item1 = new Item(null, 31, 1) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };
    Item item2 = new Item(null, 31, 12312) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };

    assertEquals(item1.hashCode(), item2.hashCode());
  }

  @Test
  void compare() {
    Item item1 = new Item(null, 31, 1) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };
    Item item2 = new Item(null, 31, 12312) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };
    Item item3 = new Item(null, 35, 12312) {
      @Override
      public Thumbnail getThumbnail() {
        return null;
      }

      @Override
      public void purgeThumbnail() {

      }
    };

    assertEquals(0, item1.compareTo(item2));
    assertEquals(0, item2.compareTo(item1));

    assertTrue(item1.compareTo(item3) < 0);
    assertTrue(item3.compareTo(item1) > 0);
  }

}
