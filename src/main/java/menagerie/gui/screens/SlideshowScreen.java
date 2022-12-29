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

package menagerie.gui.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.screens.dialogs.ConfirmationScreen;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.listeners.ObjectListener;
import menagerie.util.listeners.PokeListener;

public class SlideshowScreen extends Screen {

  private final DynamicMediaView mediaView = new DynamicMediaView();
  private final ItemInfoBox infoBox = new ItemInfoBox();
  private final Label totalLabel = new Label("0");
  private final TextField indexTextField = new TextField("/0");
  private final Button playPauseButton = new Button("Play");

  private final List<Item> items = new ArrayList<>();
  private Item showing = null;
  private Menagerie menagerie;

  private final Timer timer = new Timer(true);
  private TimerTask currentTimerTask = null;
  private final DoubleProperty interval = new SimpleDoubleProperty(10);
  private final BooleanProperty preload = new SimpleBooleanProperty(true);
  private Image preloadPrev = null, preloadNext = null; // REENG. remove?

  public SlideshowScreen(ObjectListener<Item> selectListener) {
    registerKeyEvents();
    getStyleClass().addAll(ROOT_STYLE_CLASS);

    configureUiElements(selectListener);
  }

  private void configureUiElements(ObjectListener<Item> selectListener) {
    mediaView.setRepeat(true);
    mediaView.setMute(false);
    infoBox.setMaxWidth(USE_PREF_SIZE);
    infoBox.setAlignment(Pos.BOTTOM_RIGHT);
    infoBox.setOpacity(0.75);
    BorderPane.setAlignment(infoBox, Pos.BOTTOM_RIGHT);
    BorderPane.setMargin(infoBox, new Insets(5));
    BorderPane bp = new BorderPane(null, null, null, infoBox, null);
    bp.setPickOnBounds(false);
    StackPane sp = new StackPane(mediaView, bp);
    setCenter(sp);

    Button left = getLeftButton();
    Button select = getSelectButton(selectListener);
    Button right = getRightButton();
    configurePlayPauseButton();
    Button close = getCloseButton();
    Button shuffle = getShuffleButton();
    Button reverse = getReverseButton();

    configureIndexTextField();
    indexTextField.setPrefWidth(50);
    indexTextField.setAlignment(Pos.CENTER_RIGHT);
    HBox countHBox = new HBox(indexTextField, totalLabel);
    countHBox.setAlignment(Pos.CENTER);
    HBox h = new HBox(5, left, select, playPauseButton, right, countHBox);
    h.setAlignment(Pos.CENTER);
    bp = new BorderPane(h, null, close, null, new HBox(5, shuffle, reverse));
    bp.setPadding(new Insets(5));
    setBottom(bp);
  }

  private Button getReverseButton() {
    Button reverse = new Button("Reverse");
    reverse.setOnAction(event -> reverse());
    return reverse;
  }

  private Button getShuffleButton() {
    Button shuffle = new Button("Shuffle");
    shuffle.setOnAction(event -> shuffle());
    return shuffle;
  }

  private Button getCloseButton() {
    Button close = new Button("Close");
    close.setOnAction(event -> close());
    return close;
  }

  private void configurePlayPauseButton() {
    playPauseButton.setOnAction(event -> {
      if (currentTimerTask == null) {
        startSlideShow();
        playPauseButton.setText("Pause");
      } else {
        currentTimerTask.cancel();
        currentTimerTask = null;
        playPauseButton.setText("Play");
      }
    });
  }

  private Button getRightButton() {
    Button right = new Button("->");
    right.setOnAction(event -> previewNext());
    return right;
  }

  private Button getSelectButton(ObjectListener<Item> selectListener) {
    Button select = new Button("Select");
    select.setOnAction(event -> {
      if (showing != null && selectListener != null) {
        selectListener.pass(showing);
      }
    });
    return select;
  }

  private Button getLeftButton() {
    Button left = new Button("<-");
    left.setOnAction(event -> previewLast());
    return left;
  }

  private void configureIndexTextField() {
    indexTextField.setOnAction(event -> {
      int i = items.indexOf(showing);
      try {
        int temp = Integer.parseInt(indexTextField.getText()) - 1;
        i = Math.max(0, Math.min(temp, items.size() - 1)); // Clamp to valid indices
      } catch (NumberFormatException e) {
        // Nothing
      }

      preview(items.get(i));
      requestFocus();
    });
  }

  private void registerKeyEvents() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.LEFT) {
        previewLast();
        event.consume();
      } else if (event.getCode() == KeyCode.RIGHT) {
        previewNext();
        event.consume();
      } else if (event.getCode() == KeyCode.ESCAPE) {
        close();
        event.consume();
      } else if (event.getCode() == KeyCode.DELETE) {
        tryDeleteCurrent(!event.isControlDown());
        event.consume();
      } else if (event.getCode() == KeyCode.END) {
        preview(items.get(items.size() - 1));
        event.consume();
      } else if (event.getCode() == KeyCode.HOME) {
        preview(items.get(0));
        event.consume();
      } else if (event.getCode() == KeyCode.R && event.isControlDown()) {
        reverse();
        event.consume();
      } else if (event.getCode() == KeyCode.S && event.isControlDown()) {
        shuffle();
        event.consume();
      }
    });
  }

  /**
   * Reverses the order of the items.
   */
  private void reverse() {
    Collections.reverse(items);
    updateCountLabel();
  }

  /**
   * Shuffles the order of the items.
   */
  private void shuffle() {
    Collections.shuffle(items);
    updateCountLabel();
    if (!items.isEmpty()) {
      preview(items.get(0));
    }
  }

  /**
   * Opens this screen in a manager.
   *
   * @param manager   Manager to open in.
   * @param menagerie Menagerie.
   * @param items     Items to display.
   */
  public void open(ScreenPane manager, Menagerie menagerie, List<Item> items) {
    this.items.clear();
    this.items.addAll(items);

    manager.open(this);

    this.menagerie = menagerie;
  }

  @Override
  protected void onOpen() {
    if (!items.isEmpty()) {
      preview(items.get(0));
    } else {
      preview(null);
    }
  }

  @Override
  protected void onClose() {
    items.clear();
    if (currentTimerTask != null) {
      currentTimerTask.cancel();
    }

    preview(null);
  }

  /**
   * @return Currently displayed item.
   */
  public Item getShowing() {
    return showing;
  }

  public ItemInfoBox getInfoBox() {
    return infoBox;
  }

  /**
   * Attempts to delete or remove the currently displayed item.
   *
   * @param deleteFile Delete the file after removing from the Menagerie.
   */
  private void tryDeleteCurrent(boolean deleteFile) {
    PokeListener onFinish = () -> {
      if (deleteFile) {
        menagerie.deleteItem(getShowing());
      } else {
        menagerie.forgetItem(getShowing());
      }

      if (items.isEmpty() || showing == null) {
        return;
      }

      final int index = items.indexOf(getShowing());
      items.remove(getShowing());

      if (!items.isEmpty()) {
        preview(items.get(Math.max(0, Math.min(index, items.size() - 1))));
      } else {
        preview(null);
        close();
      }
    };

    mediaView.stop();
    confirmDeleteFile(deleteFile, onFinish);
  }

  private void confirmDeleteFile(boolean deleteFile, PokeListener onFinish) {
    final var confirmationScreen = new ConfirmationScreen();
    if (deleteFile) {
      final var screenTitle = "Delete files";
      final var screenMessage = """
          Permanently delete selected files? (1 file)
                                    
          This action CANNOT be undone (files will be deleted)""";
      confirmationScreen.open(getManager(), screenTitle, screenMessage, onFinish, null);
    } else {
      final var screenTitle = "Forget files";
      final var screenMessage = """
          Remove selected files from database? (1 file)
                
          This action CANNOT be undone""";
      confirmationScreen.open(getManager(), screenTitle, screenMessage, onFinish, null);
    }
  }

  /**
   * Displays the given item.
   *
   * @param item Item to display.
   */
  private void preview(Item item) {
    showing = item;

    preloadPrev = null;
    preloadNext = null;
    int i = items.indexOf(showing);
    if (isPreload() && i >= 0) {
      if (i > 0) {
        Item previous = items.get(i - 1);
        if (previous instanceof MediaItem) {
          preloadPrev = ((MediaItem) previous).getImage();
        }
      }
      if (i + 1 < items.size()) {
        Item next = items.get(i + 1);
        if (next instanceof MediaItem) {
          preloadNext = ((MediaItem) next).getImage();
        }
      }
    }

    updateCountLabel();

    if (item instanceof MediaItem) {
      mediaView.preview(item);
      infoBox.setItem(item);
    } else {
      mediaView.preview(null);
    }
  }

  /**
   * Updates contents of the count label.
   */
  private void updateCountLabel() {
    if (showing != null) {
      indexTextField.setText("" + (items.indexOf(showing) + 1));
      totalLabel.setText("/" + items.size());
    } else {
      indexTextField.setText(null);
      totalLabel.setText("" + items.size());
    }
  }

  /**
   * Previews the next item in the list, if there is one.
   */
  private void previewNext() {
    if (items.isEmpty()) {
      preview(null);
      return;
    }

    if (!items.contains(showing)) {
      preview(items.get(0));
    } else {
      final int index = items.indexOf(showing);
      if (index < items.size() - 1) {
        preview(items.get(index + 1));
      }
    }
  }

  /**
   * Previews the previous item in the list, if there is one.
   */
  private void previewLast() {
    if (items.isEmpty()) {
      preview(null);
      return;
    }

    if (!items.contains(showing)) {
      preview(items.get(0));
    } else {
      final int index = items.indexOf(showing);
      if (index > 0) {
        preview(items.get(index - 1));
      }
    }
  }

  public void startSlideShow() {
    if (currentTimerTask != null) {
      currentTimerTask.cancel();
    }

    currentTimerTask = createSlideShowTimerTask();

    final long intervalMillis = (long) (getInterval() * 1000);
    timer.schedule(currentTimerTask, intervalMillis, intervalMillis);
  }

  private TimerTask createSlideShowTimerTask() {
    return new TimerTask() {
      @Override
      public void run() {
        if (showing == null) {
          cancel();
          return;
        }

        int i = items.indexOf(showing);
        if (i < 0) {
          cancel();
        } else if (i >= items.size() - 1) {
          Platform.runLater(() -> preview(items.get(0)));
        } else {
          Platform.runLater(() -> preview(items.get(i + 1)));
        }
      }

      @Override
      public boolean cancel() {
        currentTimerTask = null;
        Platform.runLater(() -> playPauseButton.setText("Play"));
        return super.cancel();
      }
    };
  }

  public DoubleProperty intervalProperty() {
    return interval;
  }

  public double getInterval() {
    return interval.get();
  }

  public void setInterval(double d) {
    interval.set(d);
  }

  public boolean isPreload() {
    return preload.get();
  }

  public BooleanProperty preloadProperty() {
    return preload;
  }

}