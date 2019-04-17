package menagerie.gui;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;

public class NoteListCell extends ListCell<String> {

    private final Label label = new Label();

    public NoteListCell() {
        setGraphic(label);
        label.setWrapText(true);
        label.maxWidthProperty().bind(maxWidthProperty());
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        label.setText(item);

        if (empty || item == null) {
            label.setTextFill(Color.WHITE);
        } else if (TagListPopup.isURL(item)) {
            label.setTextFill(Color.LIGHTBLUE);
        }
    }

}
