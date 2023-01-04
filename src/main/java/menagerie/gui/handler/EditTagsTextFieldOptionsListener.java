package menagerie.gui.handler;

import java.util.ArrayList;
import java.util.List;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.predictive.PredictiveTextFieldOptionsListener;
import menagerie.gui.util.TagUtil;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;

public class EditTagsTextFieldOptionsListener implements PredictiveTextFieldOptionsListener {

  private final Menagerie menagerie;
  private final ItemGridView itemGridView;

  public EditTagsTextFieldOptionsListener(Menagerie menagerie, ItemGridView itemGridView) {
    this.menagerie = menagerie;
    this.itemGridView = itemGridView;
  }

  @Override
  public List<String> getOptionsFor(String prefix) {
    prefix = prefix.toLowerCase();
    boolean negative = prefix.startsWith("-");
    if (negative) {
      prefix = prefix.substring(1);
    }


    List<Tag> tags;
    if (negative) {
      tags = getSelectedTags();
    } else {
      tags = new ArrayList<>(menagerie.getTags());
    }

    return TagUtil.getTop8Tags(prefix, negative, tags);
  }

  private List<Tag> getSelectedTags() {
    List<Tag> tags;
    tags = new ArrayList<>();
    for (Item item : itemGridView.getSelected()) {
      item.getTags().forEach(tag -> {
        if (!tags.contains(tag)) {
          tags.add(tag);
        }
      });
    }
    return tags;
  }

}
