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

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Paint;

/**
 * A simple Tag class.
 */
public class Tag implements Comparable<Tag> {

  /**
   * Regex defining the acceptable tag name strings.
   * <p>
   * No spaces, no newlines, only common alphanumerical characters and symbols.
   */
  public static final String NAME_REGEX = "[0-9a-zA-Z!@#$%^&*()\\-_=+\\[\\]{};:',./<>?`~]+";

  private final Menagerie menagerie;

  private final int id;
  private final String name;
  private final List<String> notes = new ArrayList<>();
  private final StringProperty color = new SimpleStringProperty(null);

  private final IntegerProperty frequency = new SimpleIntegerProperty(0);

  /**
   * Constructs a tag. No checks for uniqueness of ID are performed.
   * <p>
   * Tag name is expected to match the {@link #NAME_REGEX} field.
   *
   * @param id   ID of this tag. Must be unique within a Menagerie.
   * @param name Tag's name. Will be converted to lowercase.
   */
  public Tag(Menagerie menagerie, int id, String name, String colorCSS) {
    this.menagerie = menagerie;
    if (name == null) {
      throw new NullPointerException("Name cannot be null");
    }
    if (!name.matches(NAME_REGEX)) {
      throw new IllegalArgumentException(
          String.format("Name must match regex: \"%s\"", NAME_REGEX));
    }

    this.id = id;
    this.name = name.toLowerCase();
    this.color.set(colorCSS);

    colorProperty().addListener((observable, oldValue, newValue) -> {
      if (canStoreToDatabase()) {
        menagerie.getDatabaseManager().setTagColorAsync(getId(), newValue);
      }
    });
  }

  /**
   * @return The ID of this tag.
   */
  public int getId() {
    return id;
  }

  /**
   * @return The name of this tag. Conforms to {@link #NAME_REGEX} requirement.
   */
  public String getName() {
    return name;
  }

  /**
   * @return Frequency this tag is used by items.
   */
  public int getFrequency() {
    return frequency.get();
  }

  public IntegerProperty frequencyProperty() {
    return frequency;
  }

  /**
   * Increments the frequency of usage.
   */
  public void incrementFrequency() {
    frequency.set(getFrequency() + 1);
  }

  /**
   * Decrements the frequency of usage.
   */
  void decrementFrequency() {
    frequency.set(getFrequency() - 1);
  }

  /**
   * @return This tag's user defined notes.
   */
  public List<String> getNotes() {
    return notes;
  }

  /**
   * @return The color of this tag.
   */
  public String getColor() {
    return color.get();
  }

  public StringProperty colorProperty() {
    return color;
  }

  /**
   * Sets the color of this tag.
   *
   * @param colorCSS A string that can be interpreted as a JavaFX color.
   */
  public void setColor(String colorCSS) {
    if (colorCSS != null) {
      if (colorCSS.isEmpty()) {
        colorCSS = null;
      } else {
        try {
          Paint.valueOf(colorCSS);
        } catch (IllegalArgumentException e) {
          colorCSS = null;
        }
      }
    }

    this.color.set(colorCSS);
  }

  /**
   * Adds a note to this tag.
   *
   * @param note Note to add.
   */
  public void addNote(String note) {
    notes.add(note);

    if (canStoreToDatabase()) {
      menagerie.getDatabaseManager().addTagNoteAsync(getId(), note);
    }
  }

  /**
   * Removes a note from this tag.
   *
   * @param note Note to remove.
   * @return True if the note was removed.
   */
  public boolean removeNote(String note) {
    if (notes.remove(note)) {
      if (canStoreToDatabase()) {
        menagerie.getDatabaseManager().removeTagNoteAsync(getId(), note);
      }
      return true;
    }

    return false;
  }

  /**
   * @return True if this item is connected to a Menagerie with a database.
   */
  private boolean canStoreToDatabase() {
    return menagerie != null && menagerie.getDatabaseManager() != null;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Tag && ((Tag) obj).getId() == getId();
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(getId());
  }

  @Override
  public String toString() {
    return "Tag (" + getId() + ") \"" + getName() + "\"";
  }

  @Override
  public int compareTo(Tag o) {
    return getId() - o.getId();
  }

}
