package menagerie.gui;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridCell;


public class ImageGridCell extends GridCell<ImageInfo> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: lightgray";
    private static final String SELECTED_BG_CSS = "-fx-background-color: blue";


    final private ImageView view;


    public ImageGridCell() {
        super();

        this.view = new ImageView();
        setGraphic(view);
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);
    }

    @Override
    protected void updateItem(ImageInfo item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            view.setImage(null);
        } else {
            view.setImage(item.getThumbnail());
        }
    }

}
