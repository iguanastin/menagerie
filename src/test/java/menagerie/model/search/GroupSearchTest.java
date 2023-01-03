package menagerie.model.search;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupSearchTest {

  private MediaItem createMediaItemMock(int id, boolean isVideo, GroupItem group) {
    MediaItem mediaItemMock = mock(MediaItem.class);
    when(mediaItemMock.getId()).thenReturn(id);
    when(mediaItemMock.isImage()).thenReturn(!isVideo);
    when(mediaItemMock.isVideo()).thenReturn(isVideo);
    when(mediaItemMock.isInGroup()).thenReturn(true);
    when(mediaItemMock.getGroup()).thenReturn(group);
    group.addItem(mediaItemMock);

    return mediaItemMock;
  }

  private GroupItem createGroupItem(int id) {
    return new GroupItem(null, id, 1, "");
  }

  @Test
  void testRuleFilter() {
    GroupItem g1 = createGroupItem(1);
    GroupItem g2 = createGroupItem(2);

    // Create a list of test items
    List<Item> items = new ArrayList<>();
    items.add(createMediaItemMock(1, false, g1));
    items.add(createMediaItemMock(2, true, g1));
    items.add(createMediaItemMock(3, false, g1));
    items.add(createMediaItemMock(4, true, g2));
    items.add(createMediaItemMock(5, false, g2));

    // Test filtering by type
    Search search = new GroupSearch("type:video", g2, true, false);
    search.refreshSearch(items);
    assertEquals(1, search.getResults().size());
    assertEquals(items.get(3), search.getResults().get(0));

  }
}

