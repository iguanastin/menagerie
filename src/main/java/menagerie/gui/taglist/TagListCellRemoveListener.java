package menagerie.gui.taglist;

import java.util.Arrays;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

public class TagListCellRemoveListener implements ObjectListener<Tag> {

  private final PredictiveTextField searchTextField;

  public TagListCellRemoveListener(PredictiveTextField searchTextField) {
    this.searchTextField = searchTextField;
  }

  @Override
  public void pass(Tag tag) {
    String searchText = searchTextField.getText();
    if (searchText == null) {
      searchText = "";
    }
    searchText = searchText.trim();
    final var searchTokens = searchText.toLowerCase().split(" ");
    final var tagName = tag.getName();
    if (Arrays.asList(searchTokens).contains(tagName.toLowerCase())) {
      searchText = searchText.replaceAll("(^|\\s)" + tagName, " ");
    } else if (!Arrays.asList(searchTokens).contains("-" + tagName.toLowerCase())) {
      if (!searchText.isEmpty() && !searchText.endsWith(" ")) {
        searchText += " ";
      }
      searchText += "-" + tagName + " ";
    }
    searchTextField.setText(searchText.trim());
    searchTextField.requestFocus();
    searchTextField.positionCaret(searchText.length());
  }

}
