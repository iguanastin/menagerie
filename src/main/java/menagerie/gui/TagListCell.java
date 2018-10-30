package menagerie.gui;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import menagerie.model.menagerie.Tag;

public class TagListCell extends ListCell<Tag> {

    private Label countLabel, nameLabel;


    public TagListCell() {
        countLabel = new Label("(0)");
        nameLabel = new Label("N/A");
        setGraphic(new BorderPane(null, null, countLabel, null, nameLabel));

        //TODO: Add a small '-' button for easily removal
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
            countLabel.setText("(" + tag.computeFrequency() + ")");
            setTooltip(new Tooltip("(ID: " + tag.getId() + ") " + tag.getName()));
        }
    }

}
