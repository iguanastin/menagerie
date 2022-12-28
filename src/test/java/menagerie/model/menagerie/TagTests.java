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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class TagTests {

  @Test
  void createGet() {
    int id = 1;
    String name = "tag";
    Tag t = new Tag(null, id, name, null);
    assertEquals(id, t.getId());
    assertEquals(name, t.getName());

    id = 54321;
    name = "tag_two";
    t = new Tag(null, id, name, null);
    assertEquals(id, t.getId());
    assertEquals(name, t.getName());
  }

  @Test
  void createNullName() {
    assertThrows(NullPointerException.class, () -> new Tag(null, 1, null, null));
  }

  @Test
  void createBadName() {
    assertThrows(IllegalArgumentException.class, () -> new Tag(null, 1, "tag with spaces", null));
    assertThrows(IllegalArgumentException.class,
        () -> new Tag(null, 1, "tag with newline\n", null));
    assertThrows(IllegalArgumentException.class,
        () -> new Tag(null, 1, "tag with backslash\\", null));
  }

  @Test
  void testFrequency()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Tag t = new Tag(null, 1, "tag", null);

    assertEquals(0, t.getFrequency());

    Method incrementFrequency = Tag.class.getDeclaredMethod("incrementFrequency");
    incrementFrequency.setAccessible(true);
    incrementFrequency.invoke(t);

    assertEquals(1, t.getFrequency());

    Method decrementFrequency = Tag.class.getDeclaredMethod("decrementFrequency");
    decrementFrequency.setAccessible(true);
    decrementFrequency.invoke(t);

    assertEquals(0, t.getFrequency());
  }

  @Test
  void equality() {
    Tag t1 = new Tag(null, 1, "tag_1", null);
    Tag t2 = new Tag(null, 1, "tag_1", null);
    Tag t3 = new Tag(null, 1, "tag_2", null);
    Tag t4 = new Tag(null, 2, "tag_1", null);

    assertEquals(t1, t1);
    assertEquals(t1, t2);
    assertEquals(t1, t3);
    assertNotEquals(t1, t4);

    assertEquals(t1.hashCode(), t2.hashCode());
    assertNotEquals(t1.hashCode(), t4.hashCode());
  }

  @Test
  void compare() {
    Tag t1 = new Tag(null, 1, "tag_1", null);
    Tag t2 = new Tag(null, 1, "tag_2", null);
    Tag t3 = new Tag(null, 2, "tag_3", null);

    assertEquals(0, t1.compareTo(t1));
    assertEquals(0, t1.compareTo(t2));
    assertTrue(t1.compareTo(t3) < 0);
    assertEquals(-t1.compareTo(t3), t3.compareTo(t1));
  }

  @Test
  void notes() {
    Tag t = new Tag(null, 1, "tag", null);

    assertNotNull(t.getNotes());

    final String note1 = "test note";
    final String note2 = "test note 2";
    t.addNote(note1);
    t.addNote(note2);
    assertTrue(t.getNotes().contains(note1));
    assertTrue(t.getNotes().contains(note2));

    assertTrue(t.removeNote(note1));
    assertFalse(t.removeNote(note1));
    assertFalse(t.getNotes().contains(note1));
    assertTrue(t.getNotes().contains(note2));
  }

  @Test
  void colors() {
    Tag t = new Tag(null, 1, "tag", null);

    assertNull(t.getColor());
    t.setColor("");
    assertNull(t.getColor());

    final String color1 = "blue";
    t.setColor(color1);
    assertEquals(color1, t.getColor());
    t.setColor(color1);
    assertEquals(color1, t.getColor());

    final String color2 = "yellow";
    t.setColor(color2);
    assertEquals(color2, t.getColor());

    t.setColor(null);
    assertNull(t.getColor());
  }

  @Test
  void toStringDoesntThrow() {
    Tag t = new Tag(null, 1, "tag", null);
    assertDoesNotThrow(t::toString);
  }

}
