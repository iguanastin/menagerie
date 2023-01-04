package menagerie.gui.taglist;

import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;

public class OtherMissingTagListCell extends TagListCell {

  public OtherMissingTagListCell(Supplier<MediaItem> currentItem) {
    super(null, null);
    this.currentItem = currentItem;
  }

  private final Supplier<MediaItem> currentItem;

  private final PseudoClass otherHasPseudoClass = PseudoClass.getPseudoClass("other-missing");

  private final BooleanProperty otherMissing = new BooleanPropertyBase() {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(otherHasPseudoClass, get());
    }

    @Override
    public Object getBean() {
      return this;
    }

    @Override
    public String getName() {
      return "otherMissing";
    }
  };

  @Override
  protected void updateItem(Tag tag, boolean empty) {
    super.updateItem(tag, empty);

    otherMissing.set(tag != null && !currentItem.get().hasTag(tag));
  }

}
