package menagerie.gui;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridCell;


public class ImageGridCell extends GridCell<ImageInfo> {

    final private ImageView view;


    public ImageGridCell() {
        super();

        this.view = new ImageView();
        setGraphic(view);
        setAlignment(Pos.CENTER);
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
