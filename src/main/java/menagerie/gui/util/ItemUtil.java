package menagerie.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class ItemUtil {

  private ItemUtil() {
  }

  public static void removeFromGroup(List<Item> selected, ContextMenu cm) {
    MenuItem removeFromGroup = new MenuItem("Remove from group");
    removeFromGroup.setOnAction(event -> selected.forEach(item -> {
      if (item instanceof MediaItem && ((MediaItem) item).isInGroup()) {
        ((MediaItem) item).getGroup().removeItem((MediaItem) item);
      }
    }));
    cm.getItems().add(removeFromGroup);
  }

  public static void addGroupElements(List<Item> items) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) instanceof GroupItem) {
        items.addAll(((GroupItem) items.get(i)).getElements());
      }
    }
  }

  public static List<Item> flattenGroups(List<Item> items, boolean reversed) {
    items = new ArrayList<>(items);
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) instanceof GroupItem) {
        GroupItem group = (GroupItem) items.remove(i);
        items.addAll(i, group.getElements());
      }
    }
    if (reversed) {
      Collections.reverse(items);
    }
    return items;
  }

  public static List<MediaItem> flattenGroups(List<Item> selected) {
    List<MediaItem> items = new ArrayList<>();
    for (Item item : selected) {
      if (item instanceof MediaItem) {
        items.add((MediaItem) item);
      } else if (item instanceof GroupItem) {
        for (MediaItem element : ((GroupItem) item).getElements()) {
          final String name = element.getFile().getName().toLowerCase();
          if (FileExplorer.hasAllowedFileEnding(name)) {
            items.add(element);
          }
        }
      }
    }
    return items;
  }

  public static List<GroupItem> getGroupItems(List<Item> items) {
    List<GroupItem> groups = new ArrayList<>();
    items.forEach(item -> {
      if (item instanceof GroupItem) {
        groups.add((GroupItem) item);
      }
    });
    return groups;
  }

  public static String getFirstGroupTitle(List<Item> toGroup) {
    String title = null;
    for (Item item : toGroup) {
      if (item instanceof GroupItem) {
        title = ((GroupItem) item).getTitle();
        break;
      }
    }
    return title;
  }
}
