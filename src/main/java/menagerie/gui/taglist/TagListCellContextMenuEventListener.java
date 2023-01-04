package menagerie.gui.taglist;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.TextDialogScreen;

public class TagListCellContextMenuEventListener implements EventHandler<ContextMenuEvent> {

  private final TagListCell c;
  private final ScreenPane screenPane;


  public TagListCellContextMenuEventListener(TagListCell c, ScreenPane screenPane) {
    this.c = c;
    this.screenPane = screenPane;
  }

  @Override
  public void handle(ContextMenuEvent event) {
    MenuItem menuItem = new MenuItem("Add note");
    menuItem.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note",
        String.format("Add a note to tag '%s'", c.getItem().getName()), null,
        note -> c.getItem().addNote(note), null));
    ContextMenu m = new ContextMenu(menuItem, new SeparatorMenuItem());
    m.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

}
