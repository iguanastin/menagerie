package menagerie.model.menagerie;

import menagerie.gui.Thumbnail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
