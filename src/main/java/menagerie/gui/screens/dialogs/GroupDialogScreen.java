package menagerie.gui.screens.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.model.Settings;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.listeners.ObjectListener;

import java.util.ArrayList;
import java.util.List;

public class GroupDialogScreen extends Screen {

    private final TextField textField = new TextField();
    private final Label messageLabel = new Label("N/A");
    private final CheckBox elementTagsCheckBox = new CheckBox("Tag group with element tags");

    private Menagerie menagerie = null;
    private Settings settings = null;
    private List<Item> toGroup = null;

    private ObjectListener<GroupItem> groupListener = null;


    public GroupDialogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                confirm();
            }
        });

        // --------------------------------- Header --------------------------------------
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane top = new BorderPane(null, null, exit, new Separator(), new Label("Combine into group"));

        // --------------------------------- Center --------------------------------------
        VBox center = new VBox(5, messageLabel, textField, elementTagsCheckBox);
        center.setPadding(new Insets(5));

        // --------------------------------- Bottom --------------------------------------
        Button confirm = new Button("Confirm");
        confirm.setOnAction(event -> confirm());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, confirm, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        // -------------------------------- Root -----------------------------------------
        BorderPane root = new BorderPane(center, top, null, bottom, null);
        root.setPrefWidth(500);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(textField);
    }

    /**
     * Opens this screen in a manager.
     *
     * @param manager Manager to open in.
     * @param text    Default textfield text.
     */
    public void open(ScreenPane manager, Menagerie menagerie, Settings settings, String text, List<Item> toGroup, ObjectListener<GroupItem> groupListener) {
        this.groupListener = groupListener;
        this.menagerie = menagerie;
        this.settings = settings;
        int itemCount = 0;
        if (toGroup != null) {
            this.toGroup = new ArrayList<>(toGroup);
            for (Item item : toGroup) {
                if (item instanceof GroupItem) {
                    itemCount += ((GroupItem) item).getElements().size();
                } else {
                    itemCount++;
                }
            }
        } else {
            this.toGroup = null;
        }

        if (itemCount == 0) return;

        manager.open(this);

        textField.setText(text);
        textField.selectAll();
        messageLabel.setText(String.format("Create group with %d items", itemCount));
    }

    /**
     * Confirms this dialog.
     */
    private void confirm() {
        close();

        if (menagerie != null && settings != null && toGroup != null && !toGroup.isEmpty()) {
            GroupItem group = menagerie.createGroup(toGroup, textField.getText(), settings.getBoolean(Settings.Key.TAG_TAGME));
            if (elementTagsCheckBox.isSelected())
                group.getElements().forEach(item -> item.getTags().forEach(group::addTag));
            if (groupListener != null) groupListener.pass(group);
        }
    }

}
