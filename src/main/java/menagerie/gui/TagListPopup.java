package menagerie.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class TagListPopup extends Popup {

    private final Label nameLabel = new Label();
    private final SimpleCSSColorPicker colorPicker = new SimpleCSSColorPicker(new String[]{"#609dff", "cyan", "#22e538", "yellow", "orange", "red", "#ff7ae6", "#bf51ff"}, null);

    private final ListView<String> noteListView = new ListView<>();

    private ObjectListener<String> colorListener;

    private Tag tag = null;


    public TagListPopup(ObjectListener<String> colorListener) {
        this.colorListener = colorListener;

        VBox v = new VBox(5, nameLabel, new Separator(), noteListView, new Separator(), colorPicker);
        v.setStyle("-fx-background-color: -fx-base;");
        v.setPadding(new Insets(5));
        v.setEffect(new DropShadow());
        getContent().add(v);

        noteListView.setCellFactory(param -> {
            NoteListCell c = new NoteListCell();
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && isURL(c.getItem())) {
                    try {
                        Desktop.getDesktop().browse(new URI(c.getItem()));
                    } catch (IOException | URISyntaxException e) {
                        Main.log.log(Level.SEVERE, String.format("Exception when opening \"%s\" in browser", c.getItem()), e);
                    }
                }
            });
            MenuItem delete = new MenuItem("Delete note");
            delete.setOnAction(event -> {
                if (tag.removeNote(c.getItem())) {
                    noteListView.getItems().remove(c.getItem());
                }
            });
            ContextMenu cm = new ContextMenu(delete);
            c.setOnContextMenuRequested(event -> cm.show(c, event.getScreenX(), event.getScreenY()));
            c.maxWidthProperty().bind(noteListView.widthProperty().subtract(20));
            return c;
        });
    }

    void setTag(Tag tag) {
        this.tag = tag;

        colorPicker.setColorPickedListener(color -> {
            if (tag.setColorCSS(color)) {
                setNameLabelColor(color);
                if (colorListener != null) colorListener.pass(color);
            }
        });

        noteListView.getItems().clear();
        noteListView.getItems().addAll(tag.getNotes());

        nameLabel.setText(tag.getName());

        setNameLabelColor(tag.getColorCSS());
    }

    private void setNameLabelColor(String color) {
        if (color == null || color.isEmpty()) {
            nameLabel.setStyle(null);
        } else {
            nameLabel.setStyle(String.format("-fx-text-fill: %s;", color));
        }
    }

    static boolean isURL(String str) {
        if (str == null || str.isEmpty()) return false;

        try {
            new URI(str);
            return true;
        } catch (URISyntaxException e) {
            try {
                new URI("http://" + str);
                return true;
            } catch (URISyntaxException e1) {
                return false;
            }
        }
    }

}
