package menagerie.gui;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridCell;


public class ImageGridCell extends GridCell<ImageInfo> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: lightgray";
    private static final String SELECTED_BG_CSS = "-fx-background-color: blue";

    final private ImageView view;
    final private ImageGridView grid;


    public ImageGridCell(ImageGridView imageGridView) {
        super();

        this.grid = imageGridView;
        this.view = new ImageView();
        setGraphic(view);
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);

        setOnMousePressed(event -> {
            grid.cellMousePressed(this, event);
            event.consume();
        });
    }

    @Override
    protected void updateItem(ImageInfo item, boolean empty) {
        if (empty) {
            view.setImage(null);
        } else {
            view.setImage(item.getThumbnail());
        }

        super.updateItem(item, empty);

        if (grid.isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

}
