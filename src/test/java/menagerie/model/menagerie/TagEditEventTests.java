package menagerie.model.menagerie;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TagEditEventTests {

    private Map<Item, List<Tag>> added = new HashMap<>();
    private Map<Item, List<Tag>> removed = new HashMap<>();

    private Tag t1 = new Tag(null, 1, "tag1", null);
    private Tag t2 = new Tag(null, 2, "tag2", null);
    private Tag t3 = new Tag(null, 3, "tag3", null);
    private Tag t4 = new Tag(null, 4, "tag4", null);

    private Item i1 = new MediaItem(null, 1, 1, null);
    private Item i2 = new MediaItem(null, 2, 1, null);

    private TagEditEvent editEvent;


    TagEditEventTests() {
        i1.addTag(t1);
        i1.addTag(t2);
        added.put(i1, Arrays.asList(t1, t2));

        i2.addTag(t3);
        i2.addTag(t2);
        added.put(i2, Collections.singletonList(t2));

        removed.put(i1, Arrays.asList(t3, t4));
        removed.put(i2, Arrays.asList(t1, t4));

        // i1 before: t3, t4
        // i1 after:  t1, t2
        // i2 before: t1, t3, t4
        // i2 after:  t2, t3

        editEvent = new TagEditEvent(added, removed);
    }

    @Test
    void mapsCorrect() {
        for (Map.Entry<Item, List<Tag>> entry : added.entrySet()) {
            assertTrue(editEvent.getAdded().containsKey(entry.getKey()));

            List<Tag> tags = added.get(entry.getKey());
            assertEquals(tags.size(), entry.getValue().size());
            for (Tag tag : entry.getValue()) {
                assertTrue(tags.contains(tag));
            }
        }

        for (Map.Entry<Item, List<Tag>> entry : removed.entrySet()) {
            assertTrue(editEvent.getRemoved().containsKey(entry.getKey()));

            List<Tag> tags = removed.get(entry.getKey());
            assertEquals(tags.size(), entry.getValue().size());
            for (Tag tag : entry.getValue()) {
                assertTrue(tags.contains(tag));
            }
        }
    }

    @Test
    void revert() {
        assertTrue(i1.hasTag(t1));
        assertTrue(i1.hasTag(t2));
        assertFalse(i1.hasTag(t3));
        assertFalse(i1.hasTag(t4));

        assertFalse(i2.hasTag(t1));
        assertTrue(i2.hasTag(t2));
        assertTrue(i2.hasTag(t3));
        assertFalse(i2.hasTag(t4));

        editEvent.revertAction();

        assertFalse(i1.hasTag(t1));
        assertFalse(i1.hasTag(t2));
        assertTrue(i1.hasTag(t3));
        assertTrue(i1.hasTag(t4));

        assertTrue(i2.hasTag(t1));
        assertFalse(i2.hasTag(t2));
        assertTrue(i2.hasTag(t3));
        assertTrue(i2.hasTag(t4));
    }

}
