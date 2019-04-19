package menagerie.model.menagerie;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(IllegalArgumentException.class, () -> new Tag(null, 1, "tag with newline\n", null));
        assertThrows(IllegalArgumentException.class, () -> new Tag(null, 1, "tag with backslash\\", null));
    }

    @Test
    void testFrequency() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
        assertFalse(t.setColor(null));

        final String color1 = "blue";
        assertTrue(t.setColor(color1));
        assertEquals(color1, t.getColor());
        assertFalse(t.setColor(color1));
        assertEquals(color1, t.getColor());

        final String color2 = "yellow";
        assertTrue(t.setColor(color2));
        assertEquals(color2, t.getColor());

        assertTrue(t.setColor(null));
        assertNull(t.getColor());
    }

    @Test
    void toStringDoesntThrow() {
        Tag t = new Tag(null, 1, "tag", null);
        assertDoesNotThrow(t::toString);
    }

}
