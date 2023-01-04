package menagerie.gui.handler;

import java.util.ArrayList;
import java.util.List;
import menagerie.gui.predictive.PredictiveTextFieldOptionsListener;
import menagerie.gui.util.TagUtil;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;

public class SearchTextFieldOptionsListener implements PredictiveTextFieldOptionsListener {

  private final Menagerie menagerie;

  public SearchTextFieldOptionsListener(Menagerie menagerie) {
    this.menagerie = menagerie;
  }

  @Override
  public List<String> getOptionsFor(String prefix) {
    prefix = prefix.toLowerCase();
    boolean negative = prefix.startsWith("-");
    if (negative) {
      prefix = prefix.substring(1);
    }

    List<Tag> tags = new ArrayList<>(menagerie.getTags());

    return TagUtil.getTop8Tags(prefix, negative, tags);
  }

}
