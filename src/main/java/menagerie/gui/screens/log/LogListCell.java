package menagerie.gui.screens.log;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

public class LogListCell extends ListCell<String> {

    private Label label = new Label();


    public LogListCell() {
        super();

        label.setWrapText(true);
        label.maxWidthProperty().bind(widthProperty().subtract(15));
        setGraphic(label);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        label.setText(item);
    }
}
