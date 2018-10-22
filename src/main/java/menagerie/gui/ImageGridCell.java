package menagerie.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridCell;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.util.Iterator;


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
    }

    @Override
    protected void updateItem(ImageInfo item, boolean empty) {
        if (empty) {
            view.setImage(null);
        } else {
            view.setImage(item.getThumbnail());
            updateToolTip(item);

        }

        super.updateItem(item, empty);

        if (grid.isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

    private void updateToolTip(ImageInfo item) {
        //TODO: Put this info all into a context menu or something. It gets in the way too much

        //Find size string
        double size = item.getFile().length();
        String sizeStr;
        if (size > 1024 * 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024 / 1024) + "GB";
        else if (size > 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024) + "MB";
        else if (size > 1024) sizeStr = String.format("%.2f", size / 1024) + "KB";
        else sizeStr = String.format("%.2f", size) + "B";

        //Find res
        int width = -1, height = -1;
        try (ImageInputStream in = ImageIO.createImageInputStream(item.getFile())) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String res = width + "x" + height;
        if (width == -1 || height == -1) res = "Unknown";

        //Create tooltip
        Tooltip t = new Tooltip(item.getFile() + "\n" +
                "Size: " + sizeStr + "\n" +
                "Res: " + res);
        setTooltip(t);
    }

}
