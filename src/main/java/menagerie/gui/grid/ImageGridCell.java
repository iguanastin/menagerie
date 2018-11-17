package menagerie.gui.grid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import menagerie.model.menagerie.ImageInfo;
import org.controlsfx.control.GridCell;


public class ImageGridCell extends GridCell<ImageInfo> {

    private static final String UNSELECTED_BG_CSS = "-fx-background-color: -grid-cell-unselected-color";
    private static final String SELECTED_BG_CSS = "-fx-background-color: -grid-cell-selected-color";

    private final ImageView view;
    private final Label label;


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
    protected void updateItem(ImageInfo item, boolean empty) {
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
            if (item.isVideo()) label.setText("Video");
        }

        super.updateItem(item, empty);

        if (getGridView() != null && getGridView() instanceof ImageGridView && ((ImageGridView) getGridView()).isSelected(item)) {
            setStyle(SELECTED_BG_CSS);
        } else {
            setStyle(UNSELECTED_BG_CSS);
        }
    }

}
