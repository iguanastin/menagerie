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

package menagerie.gui.predictive;

import java.util.Collections;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class PredictiveTextField extends TextField {

  private static final String POPUP_STYLE_CLASS = "predictive-popup";

  private PredictiveTextFieldOptionsListener optionsListener;

  private final Popup popup = new Popup();
  private final VBox vBox = new VBox(5);

  private int selectedIndex = -1;

  private boolean top = true;


  public PredictiveTextField() {
    popup.getContent().add(vBox);

    vBox.getStyleClass().addAll(POPUP_STYLE_CLASS);
    vBox.setPadding(new Insets(5));

    caretPositionProperty().addListener((observable, oldValue, newValue) -> caretChanged());
    focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        popup.hide();
      }
    });

    addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressedEventFilter);
  }

  /**
   * Called when the text has changed.
   */
  private void caretChanged() {
    selectedIndex = -1;
    popup.hide();

    if (optionsListener == null || !isFocused()) {
      return;
    }

    String word = getText();
    if (word == null || word.isEmpty()) {
      return;
    }
    if (word.contains(" ")) {
      word = word.substring(0, Math.min(word.length(), getCaretPosition()));
      word = word.substring(word.lastIndexOf(' ') + 1);
    }

    List<String> options = optionsListener.getOptionsFor(word);
    if (options == null || options.isEmpty()) {
      return;
    }
    if (top) {
      Collections.reverse(options);
    }

    vBox.getChildren().clear();
    options.forEach(str -> {
      PredictivePopupCell cell = new PredictivePopupCell(str);
      cell.setOnMouseClicked(event -> acceptOption(str));
      vBox.getChildren().add(cell);
    });
    updateOptionCSSStyles();

    popup.show(getScene().getWindow(), 0, 0);
    updatePopupPosition();
  }

  /**
   * Moves the popup to be in the expected position.
   */
  private void updatePopupPosition() {
    Bounds b = localToScreen(getBoundsInLocal());
    popup.setX(b.getMinX());
    if (top) {
      popup.setY(b.getMinY() - popup.getHeight());
    } else {
      popup.setY(b.getMaxY());
    }
  }

  private void keyPressedEventFilter(KeyEvent event) {
    if (popup.isShowing()) {
      switch (event.getCode()) {
        case UP:
          eventFilterKeyUpPressed(event);
          break;
        case DOWN:
          eventFilterKeyDownPressed(event);
          break;
        case SPACE:
        case ENTER:
        case TAB:
          eventFilterKeyTabPressed(event);
          break;
        default:
          break;
      }
    }
  }

  private void eventFilterKeyTabPressed(KeyEvent event) {
    if (selectedIndex < 0) {
      if (event.isControlDown() || event.getCode() == KeyCode.TAB) {
        if (top) {
          selectedIndex = vBox.getChildren().size() - 1;
        } else {
          selectedIndex = 0;
        }
      }
    }

    if (selectedIndex >= 0) {
      acceptOption(((Label) vBox.getChildren().get(selectedIndex)).getText());

      if (event.getCode() == KeyCode.TAB) {
        event.consume();
      }
    }
  }

  private void eventFilterKeyDownPressed(KeyEvent event) {
    if (selectedIndex < 0) {
      selectedIndex = 0;
    } else if (selectedIndex < vBox.getChildren().size() - 1) {
      selectedIndex++;
    } else {
      selectedIndex = 0;
    }
    updateOptionCSSStyles();
    event.consume();
  }

  private void eventFilterKeyUpPressed(KeyEvent event) {
    if (selectedIndex < 0) {
      selectedIndex = vBox.getChildren().size() - 1;
    } else if (selectedIndex > 0) {
      selectedIndex--;
    } else {
      selectedIndex = vBox.getChildren().size() - 1;
    }
    updateOptionCSSStyles();
    event.consume();
  }

  private void acceptOption(String option) {
    if (getText() == null) {
      setText("");
    }

    String text = getText();
    int i = text.substring(0, getCaretPosition()).lastIndexOf(" ") + 1; // Always 0 or above
    setText(text.substring(0, i) + option + " " + text.substring(getCaretPosition()));

    positionCaret(i + option.length() + 1);

    popup.hide();
  }

  /**
   * Updates CSS styles of the options.
   */
  private void updateOptionCSSStyles() {
    for (int i = 0; i < vBox.getChildren().size(); i++) {
      ((PredictivePopupCell) vBox.getChildren().get(i)).setSelected(false);
      ((PredictivePopupCell) vBox.getChildren().get(i)).setSelectedUnfocused(false);

      if (i == selectedIndex) {
        ((PredictivePopupCell) vBox.getChildren().get(i)).setSelected(true);
      } else if (selectedIndex < 0 &&
                 ((i == 0 && !top) || (i == vBox.getChildren().size() - 1 && top))) {
        ((PredictivePopupCell) vBox.getChildren().get(i)).setSelectedUnfocused(true);
      }
    }
  }

  /**
   * @param optionsListener Listener that supplies options, given the partial word.
   */
  public void setOptionsListener(PredictiveTextFieldOptionsListener optionsListener) {
    this.optionsListener = optionsListener;
  }

  /**
   * Set the popup to be on top of the textField instead of on the bottom.
   *
   * @param top On top.
   */
  public void setTop(boolean top) {
    this.top = top;
  }

}
