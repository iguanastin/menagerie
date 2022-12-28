/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.gui.taglist;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import menagerie.model.menagerie.Tag;

public class TagListPopup extends Popup {

  private static final Logger LOGGER = Logger.getLogger(TagListPopup.class.getName());

  private static final String DEFAULT_STYLE_CLASS = "tag-list-popup";

  private final Label nameLabel = new Label();
  private final SimpleCSSColorPicker colorPicker = new SimpleCSSColorPicker();

  private final ListView<String> noteListView = new ListView<>();

  private Tag tag = null;

  public TagListPopup() {
    VBox v = new VBox(5, nameLabel, new Separator(), colorPicker, new Separator(), noteListView);
    v.getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    v.setPadding(new Insets(5));
    getContent().add(v);

    noteListView.setCellFactory(param -> {
      NoteListCell c = new NoteListCell();
      c.setOnMouseClicked(event -> {
        if (event.getButton() == MouseButton.PRIMARY && isURL(c.getItem())) {
          try {
            Desktop.getDesktop().browse(new URI(c.getItem()));
          } catch (IOException | URISyntaxException e) {
            try {
              Desktop.getDesktop().browse(new URI("https://" + c.getItem()));
            } catch (IOException | URISyntaxException e2) {
              LOGGER.log(Level.SEVERE,
                  String.format("Exception when opening \"%s\" in browser", c.getItem()), e2);
            }
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
      c.setOnContextMenuRequested(
          event -> cm.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY()));
      c.maxWidthProperty().bind(noteListView.widthProperty().subtract(20));
      return c;
    });
  }

  public void setTag(Tag tag) {
    this.tag = tag;

    colorPicker.setColorPickedListener(color -> {
      tag.setColor(color);
      setNameLabelColor(color);
    });

    noteListView.getItems().clear();
    noteListView.getItems().addAll(tag.getNotes());
    if (noteListView.getItems().isEmpty()) {
      noteListView.setMaxHeight(20);
    } else {
      noteListView.setMaxHeight(300);
    }

    nameLabel.setText(tag.getName());

    setNameLabelColor(tag.getColor());
  }

  private void setNameLabelColor(String color) {
    if (color == null || color.isEmpty()) {
      nameLabel.setStyle(null);
    } else {
      nameLabel.setStyle(String.format("-fx-text-fill: %s;", color));
    }
  }

  static boolean isURL(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }

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
