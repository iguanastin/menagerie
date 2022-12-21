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

package menagerie.model.menagerie;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import menagerie.gui.Thumbnail;
import menagerie.model.menagerie.db.DatabaseManager;

/**
 * Menagerie Item
 */
public abstract class Item implements Comparable<Item> {

  private static final Logger LOGGER = Logger.getLogger(Item.class.getName());

  private boolean invalidated = false;

  protected final Menagerie menagerie;
  protected final int id;
  protected long dateAdded;
  protected ObservableList<Tag> tags = FXCollections.observableArrayList();
  private final Map<String, Object> metadata = new HashMap<>();


  /**
   * ID uniqueness is not verified by this.
   *
   * @param menagerie Menagerie that owns this item.
   * @param id        Unique ID of this item.
   * @param dateAdded Date this item was added to the Menagerie.
   */
  protected Item(Menagerie menagerie, int id, long dateAdded) {
    this.menagerie = menagerie;
    this.id = id;
    this.dateAdded = dateAdded;
  }

  /**
   * @return The date this item was added to the menagerie.
   */
  public long getDateAdded() {
    return dateAdded;
  }

  /**
   * @return The unique ID of this item.
   */
  public int getId() {
    return id;
  }

  /**
   * @return The thumbnail of this item. May be null.
   */
  public abstract Thumbnail getThumbnail();

  public abstract void purgeThumbnail();

  /**
   * @return The tags this item is tagged with.
   */
  public ObservableList<Tag> getTags() {
    return tags;
  }

  /**
   * @param t Tag to find.
   * @return True if this item has the tag.
   */
  public boolean hasTag(Tag t) {
    if (t == null) {
      return false;
    }
    return tags.contains(t);
  }

  /**
   * Tries to add a tag to this item.
   *
   * @param t Tag to add.
   * @return True if this tag was added to this item. False otherwise.
   */
  public boolean addTag(Tag t) {
    if (t == null || hasTag(t)) {
      return false;
    }

    tags.add(t);
    if (!isInvalidated()) {
      t.incrementFrequency();

      if (hasDatabase()) {
        menagerie.getDatabaseManager().tagItemAsync(id, t.getId());
      }
    }

    return true;
  }

  /**
   * Tries to remove a tag from this item.
   *
   * @param t Tag to remove.
   * @return True if the tag was removed.
   */
  public boolean removeTag(Tag t) {
    if (t == null || !hasTag(t)) {
      return false;
    }

    tags.remove(t);
    if (!isInvalidated()) {
      t.decrementFrequency();

      if (hasDatabase()) {
        menagerie.getDatabaseManager().untagItemAsync(id, t.getId());
      }
    }

    return true;
  }

  /**
   * @return True if this item is connected to a Menagerie with a database.
   */
  protected boolean hasDatabase() {
    return menagerie != null && menagerie.getDatabaseManager() != null;
  }

  /**
   * @return The database backing this item's Menagerie.
   */
  protected DatabaseManager getDatabase() {
    if (hasDatabase()) {
      return menagerie.getDatabaseManager();
    } else {
      return null;
    }
  }

  /**
   * Forgets this item from the menagerie. No effect if not in a menagerie, or invalid.
   *
   * @return True if successful.
   */
  protected boolean forget() {
    LOGGER.info("Dropping item from Menagerie: " + getId());

    if (isInvalidated() || menagerie == null || !menagerie.getItems().remove(this)) {
      return false;
    }

    menagerie.itemRemoved(this);
    if (hasDatabase()) {
      getDatabase().removeItemAsync(getId());
    }
    getTags().forEach(Tag::decrementFrequency);
    invalidate();

    return true;
  }

  /**
   * Forgets this item.
   *
   * @return True if successful.
   */
  protected boolean delete() {
    return forget();
  }

  /**
   * Marks this item as invalid. It should not be used in any Menagerie.
   */
  private void invalidate() {
    invalidated = true;
  }

  /**
   * @return True if this item is invalid and should not be used in any Menagerie.
   */
  public boolean isInvalidated() {
    return invalidated;
  }

  public Menagerie getMenagerie() {
    return menagerie;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Item && ((Item) obj).getId() == getId();
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(getId());
  }

  @Override
  public int compareTo(Item o) {
    return getId() - o.getId();
  }

}
