package menagerie.model.menagerie;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupItemTests {

    @Test
    void title() {
        final String title = "title";
        GroupItem g = new GroupItem(null, 1, 0, title);

        assertEquals(title, g.getTitle());
    }

    @Test
    void addRemove() {
        GroupItem g = new GroupItem(null, 1, 0, "group");
        assertNotNull(g.getElements());
        assertTrue(g.getElements().isEmpty());

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        assertTrue(g.addItem(m1));
        assertTrue(g.addItem(m2));
        assertFalse(g.addItem(m2));
        assertTrue(g.getElements().contains(m1));
        assertTrue(g.getElements().contains(m2));

        assertFalse(g.removeItem(new MediaItem(null, 3, 0, null)));
        assertTrue(g.removeItem(m1));

        assertFalse(g.getElements().contains(m1));
        assertTrue(g.getElements().contains(m2));

        assertTrue(g.removeItem(m2));
        assertFalse(g.getElements().contains(m2));

        assertTrue(g.addItem(m2));
        g.removeAll();
        assertTrue(g.getElements().isEmpty());
    }

    @Test
    void elementPageIndex() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 1, 0, null);
        MediaItem m3 = new MediaItem(null, 1, 0, null);
        g.addItem(m1);
        g.addItem(m2);
        g.addItem(m3);
        for (int i = 0; i < g.getElements().size(); i++) {
            assertEquals(i, g.getElements().get(i).getPageIndex());
        }

        g.removeItem(m1);
        for (int i = 0; i < g.getElements().size(); i++) {
            assertEquals(i, g.getElements().get(i).getPageIndex());
        }

        g.removeItem(m3);
        for (int i = 0; i < g.getElements().size(); i++) {
            assertEquals(i, g.getElements().get(i).getPageIndex());
        }
    }

    @Test
    void elementGroupParenting() {
        GroupItem g1 = new GroupItem(null, 1, 0, "group");
        GroupItem g2 = new GroupItem(null, 2, 0, "group2");

        MediaItem m1 = new MediaItem(null, 3, 0, null);
        MediaItem m2 = new MediaItem(null, 4, 0, null);
        MediaItem m3 = new MediaItem(null, 5, 0, null);
        g1.addItem(m1);
        g1.addItem(m2);
        g1.addItem(m3);

        assertEquals(g1, m1.getGroup());
        assertEquals(g1, m2.getGroup());
        assertEquals(g1, m3.getGroup());

        g2.addItem(m1);
        assertEquals(g2, m1.getGroup());

        g1.removeItem(m2);
        assertNull(m2.getGroup());

        g1.removeAll();
        assertNull(m3.getGroup());
    }

    @Test
    void moveElements() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);
        g.addItem(m3);

        assertTrue(g.moveElements(Collections.singletonList(m1), m2, false));
        assertEquals(m2, g.getElements().get(0));
        assertEquals(m1, g.getElements().get(1));
        assertEquals(m3, g.getElements().get(2));

        assertTrue(g.moveElements(Collections.singletonList(m2), m3, true));
        assertEquals(m1, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
        assertEquals(m3, g.getElements().get(2));

        assertTrue(g.moveElements(Collections.singletonList(m1), m3, false));
        assertEquals(m2, g.getElements().get(0));
        assertEquals(m3, g.getElements().get(1));
        assertEquals(m1, g.getElements().get(2));
    }

    @Test
    void moveElementsToSelf() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);
        g.addItem(m3);

        assertFalse(g.moveElements(Collections.singletonList(m1), m1, true));
        assertEquals(m1, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
        assertEquals(m3, g.getElements().get(2));

        assertFalse(g.moveElements(Collections.singletonList(m1), m1, false));
        assertEquals(m1, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
        assertEquals(m3, g.getElements().get(2));
    }

    @Test
    void moveElementsMultiple() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);
        g.addItem(m3);
        List<MediaItem> list = new ArrayList<>();
        list.add(m1);
        list.add(m2);

        assertTrue(g.moveElements(list, m3, false));
        assertEquals(m3, g.getElements().get(0));
        assertEquals(m1, g.getElements().get(1));
        assertEquals(m2, g.getElements().get(2));
    }

    @Test
    void moveElementsMultipleReverse() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);
        g.addItem(m3);
        List<MediaItem> list = new ArrayList<>();
        list.add(m2);
        list.add(m1);

        assertTrue(g.moveElements(list, m3, false));
        assertEquals(m3, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
        assertEquals(m1, g.getElements().get(2));
    }

    @Test
    void moveElementsNotInGroup() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);

        assertFalse(g.moveElements(Collections.singletonList(m3), m1, false));
        assertEquals(m1, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
    }

    @Test
    void moveElementsAnchorNotInGroup() {
        GroupItem g = new GroupItem(null, 1, 0, "group");

        MediaItem m1 = new MediaItem(null, 1, 0, null);
        MediaItem m2 = new MediaItem(null, 2, 0, null);
        MediaItem m3 = new MediaItem(null, 3, 0, null);
        g.addItem(m1);
        g.addItem(m2);

        assertFalse(g.moveElements(Collections.singletonList(m1), m3, true));
        assertEquals(m1, g.getElements().get(0));
        assertEquals(m2, g.getElements().get(1));
    }

    @Test
    void toStringDoesNotThrow() {
        assertDoesNotThrow(() -> new GroupItem(null, 1, 0, "group").toString());
    }

    @Test
    void thumbnail() {
        GroupItem g = new GroupItem(null, 1, 0, "group");
        assertNull(g.getThumbnail());

        MediaItem m = new MediaItem(null, 1, 0, null);
        g.addItem(m);

        assertEquals(m.getThumbnail(), g.getThumbnail());

        g.removeItem(m);
        //        MediaItem m2 = new MediaItem(null, 1, 0, new File("white.png")); // TODO get real path to test resource
        //        g.addItem(m2);
        //        assertEquals(m2.getThumbnail(), g.getThumbnail());
    }

}
