package menagerie.model.search.rules;

import menagerie.gui.Thumbnail;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;

import java.util.Collection;

/**
 * Item implementation for
 */
public class TestItem extends Item {

  public TestItem(Menagerie menagerie, int id, long dateAdded) {
    super(menagerie, id, dateAdded);
  }

  @Override
  public Thumbnail getThumbnail() {
    return null;
  }

  @Override
  public void purgeThumbnail() {
    // nothing
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setDateAdded(long dateAdded) {
    this.dateAdded = dateAdded;
  }

  public void setTags(Collection<Tag> tags) {
    this.tags.addAll(tags);
  }
}
