package menagerie.gui.screens.log;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class LogListCell extends ListCell<LogItem> {

    private Label label = new Label();


    public LogListCell() {
        super();

        label.setWrapText(true);
        label.maxWidthProperty().bind(widthProperty().subtract(15));
        setGraphic(label);

        setOnContextMenuRequested(event -> {
            if (label.getText() != null) {
                MenuItem copy = new MenuItem("Copy");
                copy.setOnAction(event1 -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(label.getText());
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                new ContextMenu(copy).show(this, event.getScreenX(), event.getScreenY());
            }
        });
    }

    @Override
    protected void updateItem(LogItem item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null) {
            label.setText(item.getText());
            label.setStyle(item.getCSS());
        } else {
            label.setText(null);
            label.setStyle(null);
        }
    }
}
