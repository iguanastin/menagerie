package menagerie.model.search;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchTest {

    private MediaItem createMediaItemMock(int id, boolean isVideo) {
        MediaItem mediaItemMock = mock(MediaItem.class);
        when(mediaItemMock.getId()).thenReturn(id);
        when(mediaItemMock.isImage()).thenReturn(!isVideo);
        when(mediaItemMock.isVideo()).thenReturn(isVideo);

        return mediaItemMock;
    }

    @Test
    void testApplyRules() {
        // Create a list of test items
        List<Item> items = new ArrayList<>();
        items.add(createMediaItemMock(1, false));
        items.add(createMediaItemMock(2, true));
        items.add(createMediaItemMock(3, false));
        items.add(createMediaItemMock(4, true));
        items.add(createMediaItemMock(5, false));

        // Test filtering by type
        Search search = new Search("type:video", false, true, false);
        search.refreshSearch(items);
        assertEquals(2, search.getResults().size());
        assertEquals(items.get(1), search.getResults().get(0));
        assertEquals(items.get(3), search.getResults().get(1));

        // Test filtering by id
        search = new Search("id:>2", false, true, false);
        search.refreshSearch(items);
        assertEquals(3, search.getResults().size());
        assertEquals(items.get(2), search.getResults().get(0));
        assertEquals(items.get(3), search.getResults().get(1));
        assertEquals(items.get(4), search.getResults().get(2));

        // Test filtering by type and id
        search = new Search("type:video id:>2", false, true, false);
        search.refreshSearch(items);
        assertEquals(1, search.getResults().size());
        assertEquals(items.get(3), search.getResults().get(0));

    }
}

