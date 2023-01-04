package menagerie.gui.taglist;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.TextDialogScreen;

public class TagListCellContextMenuListener implements EventHandler<ContextMenuEvent> {

  private final TagListCell c;
  private final ScreenPane screenPane;

  private final EventHandler<ActionEvent> tagSelectedAction;

  private final EventHandler<ActionEvent> untagSelectedAction;

  public TagListCellContextMenuListener(TagListCell c, ScreenPane screenPane,
                                        EventHandler<ActionEvent> tagSelectedAction,
                                        EventHandler<ActionEvent> untagSelectedAction) {
    this.c = c;
    this.screenPane = screenPane;
    this.tagSelectedAction = tagSelectedAction;
    this.untagSelectedAction = untagSelectedAction;
  }

  @Override
  public void handle(ContextMenuEvent event) {
    // TODO: Make a custom popup that encompasses all the tag controls?
    if (c.getItem() != null) {
      // TODO Transfer all of this to TagListCell class and use listeners/callbacks
      MenuItem addNote = new MenuItem("Add note");
      addNote.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note",
          String.format("Add a note to tag '%s'", c.getItem().getName()), null,
          note -> c.getItem().addNote(note), null));
      MenuItem tagSelected = new MenuItem("Tag selected");
      tagSelected.setOnAction(tagSelectedAction);
      MenuItem untagSelected = new MenuItem("Untag selected");
      untagSelected.setOnAction(untagSelectedAction);
      SimpleCSSColorPicker colorPicker =
          new SimpleCSSColorPicker(color -> c.getItem().setColor(color));
      colorPicker.getTextfield().setText(c.getItem().getColor());
      CustomMenuItem tagColorPicker = new CustomMenuItem(colorPicker, false);

      ContextMenu m =
          new ContextMenu(addNote, new SeparatorMenuItem(), tagSelected, untagSelected,
              new SeparatorMenuItem(), tagColorPicker, new SeparatorMenuItem());

      // TODO: Do this better, jfc
      for (String note : c.getItem().getNotes()) {
        MenuItem item = new MenuItem(note);
        item.setMnemonicParsing(false);
        item.setOnAction(event1 -> {
          try {
            Desktop.getDesktop().browse(new URI(note));
          } catch (IOException | URISyntaxException e) {
            try {
              Desktop.getDesktop().browse(new URI("https://" + c.getItem()));
            } catch (IOException | URISyntaxException ignore) {
            }
          }
        });
        m.getItems().add(item);
      }

      m.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    }
  }

}
