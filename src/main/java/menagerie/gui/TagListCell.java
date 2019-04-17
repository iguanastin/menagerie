package menagerie.gui;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import menagerie.model.menagerie.Tag;

public class TagListCell extends ListCell<Tag> {

    private final Label countLabel;
    private final Label nameLabel;


    public TagListCell() {
        countLabel = new Label();
        countLabel.setMinWidth(USE_PREF_SIZE);
        nameLabel = new Label();
        BorderPane bp = new BorderPane(null, null, countLabel, null, nameLabel);
        setGraphic(bp);
        nameLabel.maxWidthProperty().bind(bp.widthProperty().subtract(countLabel.widthProperty()).subtract(15));
    }

    @Override
    protected void updateItem(Tag tag, boolean empty) {
        super.updateItem(tag, empty);

        if (empty) {
            nameLabel.setText(null);
            countLabel.setText(null);
            setTooltip(null);
        } else {
            nameLabel.setText(tag.getName());
            countLabel.setText("(" + tag.getFrequency() + ")");
            setTooltip(new Tooltip("(ID: " + tag.getId() + ") " + tag.getName()));
        }

        updateTextColor();
    }

    void updateTextColor() {
        if (isEmpty() || getItem() == null || getItem().getColorCSS() == null) {
            nameLabel.setTextFill(Color.WHITE);
            countLabel.setTextFill(Color.WHITE);
        } else {
            nameLabel.setTextFill(Paint.valueOf(getItem().getColorCSS()));
            countLabel.setTextFill(Paint.valueOf(getItem().getColorCSS()));
        }
    }

}
