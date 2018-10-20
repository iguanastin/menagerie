package menagerie.gui;

import menagerie.model.ImageInfo;
import org.controlsfx.control.GridView;

public class ImageGridView extends GridView<ImageInfo> {

    private static final int CELL_BORDER = 5;


    public ImageGridView() {
        setCellFactory(param -> new ImageGridCell());

        setCellWidth(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER*2);
        setCellHeight(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER*2);
    }

}
