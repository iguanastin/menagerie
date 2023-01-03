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

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.search.Search;

/**
 * Menagerie system. Contains items, manages database.
 */
public class Menagerie {

  private static final Logger LOGGER = Logger.getLogger(Menagerie.class.getName());

  // ------------------------------ Variables -----------------------------------

  private final List<Item> items = new ArrayList<>();
  private final Set<File> fileSet = new HashSet<>();
  private final List<Tag> tags = new ArrayList<>();
  private final Set<SimilarPair<MediaItem>> nonDuplicates = new HashSet<>();

  private int nextItemID;
  private int nextTagID;

  private final DatabaseManager databaseManager;

  private final List<Search> activeSearches = new ArrayList<>();


  /**
   * Constructs a Menagerie. Starts a database updater thread, loads tags and media info from database, prunes database.
   *
   * @param databaseManager Database manager to back this menagerie.
   * @throws SQLException If any errors occur in database loading/prep.
   */
  public Menagerie(DatabaseManager databaseManager) throws SQLException {
    this.databaseManager = databaseManager;

    // Load data from database
    LOGGER.info("Loading Menagerie data from database...");
    databaseManager.loadIntoMenagerie(this);

    clearUnusedTags();

    nextItemID = databaseManager.getHighestItemID() + 1;
    nextTagID = databaseManager.getHighestTagID() + 1;

    for (Item item : items) {
      if (item instanceof MediaItem) {
        fileSet.add(((MediaItem) item).getFile());
      }
    }
  }

  /**
   * Removes all unused tags from the database.
   *
   * @throws SQLException If any error occurs in database.
   */
  private void clearUnusedTags() throws SQLException {
    LOGGER.info("Removing unused tags...");
    Set<Integer> usedTags = new HashSet<>();
    for (Item img : items) {
      for (Tag t : img.getTags()) {
        usedTags.add(t.getId());
      }
    }
    for (Tag t : new ArrayList<>(tags)) {
      if (!usedTags.contains(t.getId())) {
        LOGGER.info("Removing tag: " + t);
        tags.remove(t);
        getDatabaseManager().deleteTag(t.getId());
      }
    }
  }

  /**
   * Attempts to import a file into this Menagerie. Will fail if file is already present.
   *
   * @param file File to import.
   * @return The MediaItem for the imported file, or null if import failed.
   */
  public MediaItem importFile(File file) {
    if (isFilePresent(file)) {
      return null;
    }

    LOGGER.info("Importing file to Menagerie: " + file);

    MediaItem media = new MediaItem(this, nextItemID, System.currentTimeMillis(), file);

    // Add media and commit to database
    items.add(media);
    fileSet.add(file);
    nextItemID++;
    try {
      getDatabaseManager().createMedia(media);
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to create media in database: " + media, e);
      return null;
    }

    //Update active searches
    refreshInSearches(media);

    return media;
  }

  /**
   * Creates a group and adds elements to it.
   *
   * @param elements Elements to be added. If an element is a GroupItem, that group's contents will be removed and added to the new group.
   * @param title    Title of the new group.
   * @return The newly created group. Null if title is null or empty, and null if element list is null or empty.
   */
  public GroupItem createGroup(List<Item> elements, String title) {
    if (title == null || title.isEmpty()) {
      return null;
    }

    LOGGER.info("Creating group in Menagerie: \"" + title + "\"");

    if (elements == null) {
      elements = new ArrayList<>();
    }

    GroupItem group = null;
    for (Item item : elements) {
      if (item instanceof GroupItem) {
        group = (GroupItem) item;
      }
    }
    if (group != null) {
      group.setTitle(title);
      elements.remove(group);
    } else {
      group = new GroupItem(this, nextItemID, System.currentTimeMillis(), title);

      try {
        getDatabaseManager().createGroup(group);
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error storing group in database: " + group, e);
        return null;
      }

      nextItemID++;
      items.add(group);
    }

    for (Item item : elements) {
      if (item instanceof MediaItem) {
        group.addItem((MediaItem) item);
      } else if (item instanceof ArchiveItem) {
        // Don't touch it
        // TODO
      } else if (item instanceof GroupItem) {
        List<MediaItem> e = new ArrayList<>(((GroupItem) item).getElements());
        item.getTags().forEach(group::addTag);
        forgetItem(item);
        e.forEach(group::addItem);
      } else {
        return null;
      }
    }

    // Update searches
    refreshInSearches(group);

    return group;
  }

  /**
   * Creates a new tag in this Menagerie.
   *
   * @param name Name of new tag. Must be unique (case insensitive).
   * @return The newly created tag, or null if name is not unique or name is invalid.
   */
  public Tag createTag(String name) {
    LOGGER.info("Creating tag in Menagerie: " + name);

    Tag t;
    try {
      t = new Tag(this, nextTagID, name, null);
    } catch (IllegalArgumentException e) {
      return null;
    }
    nextTagID++;

    tags.add(t);

    getDatabaseManager().createTagAsync(t.getId(), t.getName());

    return t;
  }

  /**
   * Forgets a set of items.
   *
   * @param items Items
   */
  public void forgetItems(List<Item> items) {
    LOGGER.info("Forgetting " + items.size() + " items from Menagerie");
    items.forEach(Item::forget);

    refreshInSearches(items);
  }

  /**
   * Forgets an item.
   *
   * @param item Item
   */
  public void forgetItem(Item item) {
    item.forget();

    refreshInSearches(item);
  }

  /**
   * Deletes a set of items.
   *
   * @param items Items
   */
  public void deleteItems(List<Item> items) {
    LOGGER.info("Deleting " + items.size() + " items from Menagerie");
    items.forEach(Item::delete);

    refreshInSearches(items);
  }

  /**
   * Deletes an item.
   *
   * @param item Item
   */
  public void deleteItem(Item item) {
    item.delete();

    refreshInSearches(item);
  }

  /**
   * @return All tags in the Menagerie environment.
   */
  public List<Tag> getTags() {
    return tags;
  }

  /**
   * Attempts to find a tag given a tag id.
   *
   * @param id ID of tag to find.
   * @return Tag with given ID, or null if none exist.
   */
  public Tag getTagByID(int id) {
    for (Tag t : tags) {
      if (t.getId() == id) {
        return t;
      }
    }
    return null;
  }

  /**
   * Attempts to find a tag given a tag name.
   * <p>
   * Case-insensitive.
   *
   * @param name Name of tag to find.
   * @return Tag with given name, or null if none exist.
   */
  public Tag getTagByName(String name) {
    name = name.replace(' ', '_');
    for (Tag t : tags) {
      if (t.getName().equalsIgnoreCase(name)) {
        return t;
      }
    }
    return null;
  }

  public Set<SimilarPair<MediaItem>> getNonDuplicates() {
    return nonDuplicates;
  }

  public boolean hasNonDuplicate(SimilarPair<MediaItem> pair) {
    return nonDuplicates.contains(pair);
  }

  public boolean addNonDuplicate(SimilarPair<MediaItem> pair) {
    if (nonDuplicates.contains(pair)) {
      return false;
    }

    databaseManager.addNonDuplicateAsync(pair.getObject1().getId(), pair.getObject2().getId());

    return nonDuplicates.add(pair);
  }

  public boolean removeNonDuplicate(SimilarPair<MediaItem> pair) {
    if (!nonDuplicates.contains(pair)) {
      return false;
    }

    databaseManager.removeNonDuplicateAsync(pair.getObject1().getId(), pair.getObject2().getId());

    return nonDuplicates.remove(pair);
  }

  /**
   * Adds items to any searches that they are valid in, removes them from any searches they are not valid in.
   *
   * @param items Items to check.
   */
  public void refreshInSearches(List<Item> items) {
    activeSearches.forEach(search -> Platform.runLater(() -> search.refreshSearch(items)));
  }

  /**
   * Adds the item to any searches that it is valid in, removes it from any searches it is not valid in.
   *
   * @param item Item to check.
   */
  public void refreshInSearches(Item item) {
    refreshInSearches(Collections.singletonList(item));
  }

  /**
   * @return All items in this Menagerie.
   */
  public List<Item> getItems() {
    return items;
  }

  public Item getItemByID(int id) {
    for (Item item : items) {
      if (item.getId() == id) {
        return item;
      }
    }

    return null;
  }

  /**
   * @return The database updater thread backing this Menagerie.
   */
  public DatabaseManager getDatabaseManager() {
    return databaseManager;
  }

  /**
   * @param file File to search for.
   * @return True if this file has already been imported into this Menagerie.
   */
  public boolean isFilePresent(File file) {
    return fileSet.contains(file);
  }

  /**
   * Unregisters a search from this Menagerie.
   *
   * @param search Search to unregister.
   */
  public void unregisterSearch(Search search) {
    LOGGER.info("Unregistering search from Menagerie: " + search.getSearchString());
    activeSearches.remove(search);
  }

  /**
   * Registers a search to this Menagerie so it will receive updates when items are modified.
   *
   * @param search Search to register.
   */
  public void registerSearch(Search search) {
    LOGGER.info("Registering search with Menagerie: " + search.getSearchString());
    activeSearches.add(search);
  }

  /**
   * Called by items when they removed themselves from the menagerie.
   *
   * @param item Item that was removed.
   */
  void itemRemoved(Item item) {
    if (item instanceof MediaItem) {
      fileSet.remove(((MediaItem) item).getFile());
    }
  }

}
