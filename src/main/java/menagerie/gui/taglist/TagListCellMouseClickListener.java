package menagerie.gui.taglist;

import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import menagerie.gui.predictive.PredictiveTextField;

public class TagListCellMouseClickListener implements EventHandler<MouseEvent> {

  private final TagListCell c;
  private final PredictiveTextField searchTextField;

  public TagListCellMouseClickListener(TagListCell c, PredictiveTextField searchTextField) {
    this.c = c;
    this.searchTextField = searchTextField;
  }

  @Override
  public void handle(MouseEvent event) {
    if (event.getButton() == MouseButton.PRIMARY && c.getItem() != null) {
      searchTextField.setText(c.getItem().getName() + " ");
      searchTextField.requestFocus();
      searchTextField.positionCaret(searchTextField.getText().length());
      event.consume();
    }
  }

}
