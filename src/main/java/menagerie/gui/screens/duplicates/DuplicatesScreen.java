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

package menagerie.gui.screens.duplicates;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.taglist.TagListCell;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

public class DuplicatesScreen extends Screen {

  private final DynamicMediaView leftMediaView = new DynamicMediaView();
  private final DynamicMediaView rightMediaView = new DynamicMediaView();
  private final ListView<Tag> leftTagList = new ListView<>();
  private final ListView<Tag> rightTagList = new ListView<>();
  private final ItemInfoBox leftInfoBox = new ItemInfoBox();
  private final ItemInfoBox rightInfoBox = new ItemInfoBox();
  private final CheckBox nonDupeCheckBox = new CheckBox("Not a duplicate");

  private final Label similarityLabel = new Label("N/A");
  private final TextField indexTextField = new TextField("0");

  private Menagerie menagerie = null;
  private List<SimilarPair<MediaItem>> pairs = null;
  private SimilarPair<MediaItem> currentPair = null;

  private final BooleanProperty deleteFile = new SimpleBooleanProperty(true);
  private final BooleanProperty preload = new SimpleBooleanProperty(true);
  // REENG. remove?
  private Image preloadPrevLeft = null, preloadPrevRight = null, preloadNextLeft = null,
      preloadNextRight = null;

  private ObjectListener<Item> selectListener = null;
  private final ListChangeListener<Tag> leftTagListener = c -> {
    while (c.next()) {
      repopulateTagLists();
    }
  };
  private final ListChangeListener<Tag> rightTagListener = c -> {
    while (c.next()) {
      repopulateTagLists();
    }
  };

  public DuplicatesScreen() {
    registerKeyEvents();
    getStyleClass().addAll(ROOT_STYLE_CLASS);

    // ---------------------------------------------- Center Element -----------------------------------------------
    registerContextMenuRequestedEvent(leftMediaView, "Add tags to other -->",
        currentPair.getObject1(), currentPair.getObject2());
    registerContextMenuRequestedEvent(rightMediaView, "<-- Add tags to other",
        currentPair.getObject2(), currentPair.getObject1());
    registerCellFactory(leftTagList, currentPair.getObject2(), currentPair.getObject1());
    registerCellFactory(rightTagList, currentPair.getObject1(), currentPair.getObject2());
    configureCenterElements();
    addSplitPane();

    // ---------------------------------------- Bottom element -----------------------------------------------------
    VBox bottom = new VBox(5);
    bottom.setPadding(new Insets(5));
    constructFirstElement(bottom);
    constructSecondElement(bottom);
  }

  private void constructSecondElement(VBox bottom) {
    Button prevPairButton = new Button("<-");
    prevPairButton.setOnAction(event -> previewPrev());
    Button closeButton = new Button("Close");
    closeButton.setOnAction(event -> close());
    Button nextPairButton = new Button("->");
    nextPairButton.setOnAction(event -> previewNext());
    registerAddListenerToCheckbox();
    HBox hb = new HBox(5, prevPairButton, closeButton, nextPairButton, nonDupeCheckBox);
    hb.setAlignment(Pos.CENTER);
    bottom.getChildren().add(hb);
    setBottom(bottom);
    setDefaultFocusNode(closeButton);
  }

  private void registerAddListenerToCheckbox() {
    nonDupeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        menagerie.addNonDuplicate(currentPair);
      } else {
        menagerie.removeNonDuplicate(currentPair);
      }
    });
  }

  private void constructFirstElement(VBox bottom) {
    configureIndexTextField();
    HBox hbc = new HBox(indexTextField, similarityLabel);
    hbc.setAlignment(Pos.CENTER);
    Button leftDeleteButton = new Button("Delete");
    leftDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject1()));
    Button rightDeleteButton = new Button("Delete");
    rightDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject2()));
    HBox hbl = new HBox(leftDeleteButton);
    hbl.setAlignment(Pos.CENTER_LEFT);
    HBox hbr = new HBox(rightDeleteButton);
    hbr.setAlignment(Pos.CENTER_RIGHT);
    bottom.getChildren().add(new BorderPane(hbc, null, hbr, null, hbl));
  }

  private void configureIndexTextField() {
    indexTextField.setAlignment(Pos.CENTER_RIGHT);
    indexTextField.setPrefWidth(50);
    indexTextField.setOnAction(event -> {
      int i = pairs.indexOf(currentPair);
      try {
        int temp = Integer.parseInt(indexTextField.getText()) - 1;
        i = Math.max(0, Math.min(temp, pairs.size() - 1)); // Clamp to valid indices
      } catch (NumberFormatException e) {
        // Nothing
      }

      preview(pairs.get(i));
      requestFocus();
    });
  }

  private void addSplitPane() {
    BorderPane lbp = new BorderPane(null, null, leftTagList, null, leftInfoBox);
    lbp.setPickOnBounds(false);
    BorderPane rbp = new BorderPane(null, null, rightInfoBox, null, rightTagList);
    rbp.setPickOnBounds(false);
    SplitPane sp =
        new SplitPane(new StackPane(leftMediaView, lbp), new StackPane(rightMediaView, rbp));
    sp.setOnMouseEntered(event -> {
      lbp.setRight(leftTagList);
      rbp.setLeft(rightTagList);
    });
    sp.setOnMouseExited(event -> {
      lbp.setRight(null);
      rbp.setLeft(null);
    });
    setCenter(sp);
  }

  private void configureCenterElements() {
    leftTagList.setPrefWidth(200);
    rightTagList.setPrefWidth(200);
    leftInfoBox.setAlignment(Pos.BOTTOM_LEFT);
    leftInfoBox.setMaxHeight(USE_PREF_SIZE);
    leftInfoBox.setOpacity(0.75);
    BorderPane.setAlignment(leftInfoBox, Pos.BOTTOM_LEFT);
    rightInfoBox.setAlignment(Pos.BOTTOM_RIGHT);
    rightInfoBox.setMaxHeight(USE_PREF_SIZE);
    rightInfoBox.setOpacity(0.75);
    BorderPane.setAlignment(rightInfoBox, Pos.BOTTOM_RIGHT);
  }

  private void registerCellFactory(ListView<Tag> tagListView, MediaItem obj1, MediaItem obj2) {
    tagListView.setCellFactory(param -> {
      // TODO
      TagListCell c = new TagListCell(null, null) {
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

          otherMissing.set(tag != null && !obj1.hasTag(tag));
        }
      };

      MenuItem addToOther = new MenuItem("Add to other");
      addToOther.setOnAction(event -> obj1.addTag(c.getItem()));
      MenuItem removeTag = new MenuItem("Remove tag");
      removeTag.setOnAction(event -> obj2.removeTag(c.getItem()));
      ContextMenu cm = new ContextMenu(addToOther, new SeparatorMenuItem(), removeTag);
      c.setOnContextMenuRequested(
          event -> cm.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY()));
      return c;
    });
  }

  private void registerContextMenuRequestedEvent(DynamicMediaView mediaView, String menuItemLabel,
                                                 MediaItem obj1, MediaItem obj2) {
    mediaView.setOnContextMenuRequested(event -> {
      MenuItem select = new MenuItem("Select in explorer");
      MenuItem combineTags = new MenuItem(menuItemLabel);
      ContextMenu cm = new ContextMenu(select, combineTags);
      select.setOnAction(event1 -> {
        if (selectListener != null) {
          selectListener.pass(obj1);
        }
        cm.hide();
        close();
      });
      combineTags.setOnAction(event1 -> {
        obj1.getTags().forEach(obj2::addTag);
        cm.hide();
      });
      cm.show(mediaView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    });
  }

  private void registerKeyEvents() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        close();
        event.consume();
      } else if (event.getCode() == KeyCode.LEFT) {
        previewPrev();
        event.consume();
      } else if (event.getCode() == KeyCode.RIGHT) {
        previewNext();
        event.consume();
      } else if (event.getCode() == KeyCode.END) {
        preview(pairs.get(pairs.size() - 1));
        event.consume();
      } else if (event.getCode() == KeyCode.HOME) {
        preview(pairs.get(0));
        event.consume();
      }
    });
  }

  /**
   * Opens this screen in a manager and displays the first pair.
   *
   * @param manager   Manager to open in.
   * @param menagerie Menagerie.
   * @param pairs     Pairs to view and resolve.
   */
  public void open(ScreenPane manager, Menagerie menagerie, List<SimilarPair<MediaItem>> pairs) {
    if (manager == null || menagerie == null || pairs == null || pairs.isEmpty()) {
      return;
    }

    openWithOldPairs(manager, menagerie);

    this.pairs = pairs;
    preview(pairs.get(0));
  }

  public void openWithOldPairs(ScreenPane manager, Menagerie menagerie) {
    this.menagerie = menagerie;

    leftTagList.setDisable(false);
    rightTagList.setDisable(false);
    leftTagList.setOpacity(0.75);
    rightTagList.setOpacity(0.75);

    manager.open(this);
  }

  /**
   * Displays the next pair, if there is one.
   */
  private void previewNext() {
    if (pairs == null || pairs.isEmpty()) {
      return;
    }

    if (currentPair == null) {
      preview(pairs.get(0));
    } else {
      int i = pairs.indexOf(currentPair);
      if (i >= 0) {
        if (i + 1 < pairs.size()) {
          preview(pairs.get(i + 1));
        }
      } else {
        preview(pairs.get(0));
      }
    }
  }

  /**
   * Displays the previous pair, if there is one.
   */
  private void previewPrev() {
    if (pairs == null || pairs.isEmpty()) {
      return;
    }

    if (currentPair == null) {
      preview(pairs.get(0));
    } else {
      int i = pairs.indexOf(currentPair);
      if (i > 0) {
        preview(pairs.get(i - 1));
      } else {
        preview(pairs.get(0));
      }
    }
  }

  /**
   * Displays a pair.
   *
   * @param pair Pair to display.
   */
  private void preview(SimilarPair<MediaItem> pair) {
    if (currentPair != null) {
      currentPair.getObject1().getTags().removeListener(leftTagListener);
      currentPair.getObject2().getTags().removeListener(rightTagListener);
    }
    currentPair = pair;

    leftTagList.getItems().clear();
    rightTagList.getItems().clear();
    updatePreload(pair);

    if (pair != null) {
      previewPair(pair);
    } else {
      resetPreview();
    }
  }

  private void resetPreview() {
    leftMediaView.preview(null);
    rightMediaView.preview(null);

    leftInfoBox.setItem(null);
    rightInfoBox.setItem(null);

    similarityLabel.setText("N/A");
    nonDupeCheckBox.setSelected(false);
  }

  private void previewPair(SimilarPair<MediaItem> pair) {
    leftMediaView.preview(pair.getObject1());
    rightMediaView.preview(pair.getObject2());

    leftTagList.getItems().addAll(pair.getObject1().getTags());
    leftTagList.getItems().sort(Comparator.comparing(Tag::getName));
    currentPair.getObject1().getTags().addListener(leftTagListener);

    rightTagList.getItems().addAll(pair.getObject2().getTags());
    rightTagList.getItems().sort(Comparator.comparing(Tag::getName));
    currentPair.getObject2().getTags().addListener(rightTagListener);

    leftInfoBox.setItem(pair.getObject1());
    rightInfoBox.setItem(pair.getObject2());

    DecimalFormat df = new DecimalFormat("#.##");
    indexTextField.setText((pairs.indexOf(pair) + 1) + "");
    similarityLabel.setText(
        "/" + pairs.size() + ": " + df.format(pair.getSimilarity() * 100) + "%");

    nonDupeCheckBox.setSelected(menagerie.hasNonDuplicate(pair));
  }

  private void updatePreload(SimilarPair<MediaItem> pair) {
    preloadPrevLeft = preloadPrevRight = preloadNextLeft = preloadNextRight = null;
    if (isPreload()) {
      int i = pairs.indexOf(pair);
      if (i >= 0) {
        if (i > 0) {
          SimilarPair<MediaItem> p = pairs.get(i - 1);
          preloadPrevLeft = p.getObject1().getImage();
          preloadPrevRight = p.getObject2().getImage();
        }
        if (i < pairs.size() - 1) {
          SimilarPair<MediaItem> p = pairs.get(i + 1);
          preloadNextLeft = p.getObject1().getImage();
          preloadNextRight = p.getObject2().getImage();
        }
      }
    }
  }

  /**
   * Attempts to delete an item. WARNING: Deletes the file.
   *
   * @param toDelete Item to delete.
   */
  private void deleteItem(MediaItem toDelete) {
    if (menagerie == null) {
      return;
    }

    if (currentPair.getObject1().equals(toDelete)) {
      leftMediaView.stop();
    }
    if (currentPair.getObject2().equals(toDelete)) {
      rightMediaView.stop();
    }

    int index = pairs.indexOf(currentPair);

    if (isDeleteFile()) {
      menagerie.deleteItem(toDelete);
    } else {
      menagerie.forgetItem(toDelete);
    }

    //Remove other pairs containing the deleted image
    for (final var pair : new ArrayList<>(pairs)) {
      if (toDelete.equals(pair.getObject1()) || toDelete.equals(pair.getObject2())) {
        int i = pairs.indexOf(pair);
        pairs.remove(pair);
        if (i < index) {
          index--;
        }
      }
    }

    if (index > pairs.size() - 1) {
      index = pairs.size() - 1;
    }

    if (pairs.isEmpty()) {
      close();
    } else {
      preview(pairs.get(index));
    }
  }

  private void repopulateTagLists() {
    leftTagList.getItems().clear();
    if (currentPair != null) {
      leftTagList.getItems().addAll(currentPair.getObject1().getTags());
      leftTagList.getItems().sort(Comparator.comparing(Tag::getName));
    }

    rightTagList.getItems().clear();
    if (currentPair != null) {
      rightTagList.getItems().addAll(currentPair.getObject2().getTags());
      rightTagList.getItems().sort(Comparator.comparing(Tag::getName));
    }
  }

  public List<SimilarPair<MediaItem>> getPairs() {
    return pairs;
  }

  private boolean isDeleteFile() {
    return deleteFile.get();
  }

  private boolean isPreload() {
    return preload.get();
  }

  public BooleanProperty preloadProperty() {
    return preload;
  }

  /**
   * @param selectListener Listener waiting for the user to ask to select an item in the explorer.
   */
  public void setSelectListener(ObjectListener<Item> selectListener) {
    this.selectListener = selectListener;
  }

  public ItemInfoBox getLeftInfoBox() {
    return leftInfoBox;
  }

  public ItemInfoBox getRightInfoBox() {
    return rightInfoBox;
  }

}
