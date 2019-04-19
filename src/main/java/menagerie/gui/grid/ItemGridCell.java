package menagerie.gui.grid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.listeners.ObjectListener;
import org.controlsfx.control.GridCell;


public class ItemGridCell extends GridCell<Item> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: -grid-cell-unselected-color";
    private static final String SELECTED_BG_CSS = "-fx-background-color: -grid-cell-selected-color";

    private static final Font largeFont = Font.font(Font.getDefault().getName(), FontWeight.BOLD, 28);
    private static final Font smallFont = Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14);

    private final ImageView view = new ImageView();
    private final Label centerLabel = new Label();
    private final Label bottomRightLabel = new Label();

    private Item lastItem = null;

    private final ObjectListener<Image> imageReadyListener;


    public ItemGridCell() {
        super();
        this.getStyleClass().add("image-grid-cell");

        centerLabel.setPadding(new Insets(5));
        centerLabel.setFont(largeFont);
        centerLabel.setEffect(new DropShadow());
        centerLabel.setWrapText(true);

        bottomRightLabel.setPadding(new Insets(2));
        bottomRightLabel.setFont(smallFont);
        bottomRightLabel.setEffect(new DropShadow());
        StackPane.setAlignment(bottomRightLabel, Pos.BOTTOM_RIGHT);

        setGraphic(new StackPane(view, centerLabel, bottomRightLabel));
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);

        imageReadyListener = view::setImage;

    }

    @Override
    protected void updateItem(Item item, boolean empty) {
        if (lastItem != null && lastItem.getThumbnail() != null)
            lastItem.getThumbnail().removeImageReadyListener(imageReadyListener);
        lastItem = item;

        if (empty) {
            view.setImage(null);
            centerLabel.setText(null);
            bottomRightLabel.setText(null);
        } else {
            if (item.getThumbnail() != null) {
                if (item.getThumbnail().getImage() != null) {
                    view.setImage(item.getThumbnail().getImage());
                } else {
                    view.setImage(null);
                    item.getThumbnail().addImageReadyListener(imageReadyListener);
                }
            }
            if (item instanceof MediaItem) {
                if (((MediaItem) item).isVideo()) {
                    centerLabel.setText("Video");
                    centerLabel.setFont(largeFont);
                } else {
                    centerLabel.setText(null);
                }
                if (((MediaItem) item).inGroup()) {
                    bottomRightLabel.setText(((MediaItem) item).getPageIndex() + "");
                } else {
                    bottomRightLabel.setText(null);
                }
            } else if (item instanceof GroupItem) {
                centerLabel.setText(((GroupItem) item).getTitle());
                centerLabel.setFont(smallFont);
                bottomRightLabel.setText(((GroupItem) item).getElements().size() + "");
            }
        }

        super.updateItem(item, empty);

        if (getGridView() != null && getGridView() instanceof ItemGridView && ((ItemGridView) getGridView()).isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

}
