package menagerie.gui.util;

import javafx.scene.layout.BorderPane;
import menagerie.gui.Thumbnail;
import menagerie.gui.grid.ItemGridView;

public class GridPaneUtil {

  private GridPaneUtil() {
  }

  /**
   * Sets the item grid width.
   *
   * @param n Width of grid in number of cells.
   */
  public static void setGridWidth(int n, ItemGridView itemGridView, BorderPane gridPane) {
    final double width = 18 + (Thumbnail.THUMBNAIL_SIZE + ItemGridView.CELL_BORDER * 2 +
                               itemGridView.getHorizontalCellSpacing() * 2) * n;
    gridPane.setMinWidth(width);
    gridPane.setMaxWidth(width);
    gridPane.setPrefWidth(width);
  }

}
