package menagerie.gui.grid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Item;
import org.controlsfx.control.GridCell;


public class ImageGridCell extends GridCell<Item> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: -grid-cell-unselected-color";
    private static final String SELECTED_BG_CSS = "-fx-background-color: -grid-cell-selected-color";

    private final ImageView view;
    private final Label label;

    private Item lastItem = null;


    public ImageGridCell() {
        super();
        this.getStyleClass().add("image-grid-cell");

        view = new ImageView();
        label = new Label();
        label.setPadding(new Insets(5));
        label.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 28));
        label.setEffect(new DropShadow());
        setGraphic(new StackPane(view, label));
        setAlignment(Pos.CENTER);
        setStyle(UNSELECTED_BG_CSS);

    }

    @Override
    protected void updateItem(Item item, boolean empty) {
        if (lastItem != null && lastItem.getThumbnail() != null) lastItem.getThumbnail().setImageReadyListener(null);
        lastItem = item;

        if (empty) {
            view.setImage(null);
            label.setText(null);
        } else {
            if (item.getThumbnail() != null) {
                if (item.getThumbnail().getImage() != null) {
                    view.setImage(item.getThumbnail().getImage());
                } else {
                    view.setImage(null);
                    item.getThumbnail().setImageReadyListener(view::setImage);
                }
            }
            if (item instanceof MediaItem && ((MediaItem) item).isVideo()) {
                label.setText("Video");
            } else if (item instanceof GroupItem) {
                label.setText("Group");
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
