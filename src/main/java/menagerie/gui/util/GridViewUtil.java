package menagerie.gui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.input.Dragboard;
import menagerie.gui.grid.ItemGridView;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class GridViewUtil {

  private GridViewUtil() {
  }

  public static void doDragAndDrop(Dragboard db, ItemGridView itemGridView) {
    for (Item item : itemGridView.getSelected()) {
      if (item instanceof MediaItem) {
        String filename = ((MediaItem) item).getFile().getName().toLowerCase();
        if (FileExplorer.hasAllowedFileEnding(filename)) {
          if (item.getThumbnail().isLoaded()) {
            db.setDragView(item.getThumbnail().getImage());
            break;
          }
        }
      }
    }
  }

  public static List<File> getSelectedFiles(ItemGridView itemGridView) {
    List<File> files = new ArrayList<>();
    itemGridView.getSelected().forEach(item -> {
      if (item instanceof MediaItem) {
        files.add(((MediaItem) item).getFile());
      } else if (item instanceof GroupItem) {
        ((GroupItem) item).getElements()
            .forEach(mediaItem -> files.add(mediaItem.getFile()));
      }
    });
    return files;
  }

}
