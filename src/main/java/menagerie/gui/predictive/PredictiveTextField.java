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

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.Collections;
import java.util.List;

public class PredictiveTextField extends TextField {

    private static final String SELECTED_CSS = "-fx-background-color: derive(-fx-accent, 50%);";
    private static final String SELECTED_UNFOCUSED_CSS = "-fx-background-color: -fx-accent;";

    private PredictiveTextFieldOptionsListener optionsListener;

    private final Popup popup = new Popup();
    private final VBox vBox = new VBox(5);

    private int selectedIndex = -1;

    private boolean top = true;


    public PredictiveTextField() {
        popup.getContent().add(vBox);

        vBox.setStyle("-fx-background-color: -fx-base;");
        vBox.setPadding(new Insets(5));

        textProperty().addListener((observable, oldValue, newValue) -> textChanged());
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) popup.hide();
        });

        addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressedEventFilter);
    }

    /**
     * Called when the text has changed.
     */
    private void textChanged() {
        selectedIndex = -1;
        popup.hide();

        if (optionsListener == null || !isFocused()) return;

        String word = getText();
        if (word == null || word.isEmpty()) {
            return;
        }
        if (word.contains(" ")) word = word.substring(word.lastIndexOf(' ') + 1);

        List<String> options = optionsListener.getOptionsFor(word);
        if (options == null || options.isEmpty()) {
            return;
        }
        if (top) Collections.reverse(options);

        vBox.getChildren().clear();
        options.forEach(str -> {
            Label label = new Label(str);
            label.setOnMouseClicked(event -> acceptOption(str));
            vBox.getChildren().add(label);
        });
        updateOptionCSSStyles();

        popup.show(this, 0, 0);
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
                    if (selectedIndex < 0) {
                        selectedIndex = vBox.getChildren().size() - 1;
                    } else if (selectedIndex > 0) {
                        selectedIndex--;
                    } else {
                        selectedIndex = vBox.getChildren().size() - 1;
                    }
                    updateOptionCSSStyles();
                    event.consume();
                    break;
                case DOWN:
                    if (selectedIndex < 0) {
                        selectedIndex = 0;
                    } else if (selectedIndex < vBox.getChildren().size() - 1) {
                        selectedIndex++;
                    } else {
                        selectedIndex = 0;
                    }
                    updateOptionCSSStyles();
                    event.consume();
                    break;
                case SPACE:
                case ENTER:
                case TAB:
                    if (selectedIndex < 0) {
                        if (event.isControlDown() || event.getCode() == KeyCode.TAB) {
                            if (top) selectedIndex = vBox.getChildren().size() - 1;
                            else selectedIndex = 0;
                        }
                    }

                    if (selectedIndex >= 0) {
                        acceptOption(((Label) vBox.getChildren().get(selectedIndex)).getText());

                        if (event.getCode() == KeyCode.TAB) event.consume();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void acceptOption(String option) {
        if (getText() == null || getText().isEmpty() || !getText().contains(" ")) {
            setText(option + " ");
        } else {
            String temp = getText().substring(0, getText().lastIndexOf(' ') + 1);
            setText(temp + option + " ");
        }

        positionCaret(getText().length() + 1);

        popup.hide();
    }

    /**
     * Updates CSS styles of the options.
     */
    private void updateOptionCSSStyles() {
        for (int i = 0; i < vBox.getChildren().size(); i++) {
            if (i == selectedIndex) {
                vBox.getChildren().get(i).setStyle(SELECTED_CSS);
            } else if (selectedIndex < 0 && ((i == 0 && !top) || (i == vBox.getChildren().size() - 1 && top))) {
                vBox.getChildren().get(i).setStyle(SELECTED_UNFOCUSED_CSS);
            } else {
                vBox.getChildren().get(i).setStyle(null);
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
     * @return Options listener.
     */
    public PredictiveTextFieldOptionsListener getOptionsListener() {
        return optionsListener;
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
