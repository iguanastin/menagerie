package menagerie.gui.predictive;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.Collections;
import java.util.List;

public class PredictiveTextField extends TextField {

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
        options.forEach(str -> vBox.getChildren().add(new Label(str)));
        updateOptionCSSStyles();

        popup.show(this, 0, 0);
        updatePopupPosition();
    }

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
                    if (event.isControlDown()) {
                        if (top) selectedIndex = vBox.getChildren().size() - 1;
                        else selectedIndex = 0;
                    }
                    if (selectedIndex >= 0) {
                        if (getText() == null || getText().isEmpty() || !getText().contains(" ")) {
                            setText(((Label) vBox.getChildren().get(selectedIndex)).getText() + " ");
                        } else {
                            String temp = getText().substring(0, getText().lastIndexOf(' ') + 1);
                            setText(temp + ((Label) vBox.getChildren().get(selectedIndex)).getText() + " ");
                        }

                        positionCaret(getText().length() + 1);

                        popup.hide();
                    }
                    break;
            }
        }
    }

    private void updateOptionCSSStyles() {
        for (int i = 0; i < vBox.getChildren().size(); i++) {
            if (i == selectedIndex) {
                vBox.getChildren().get(i).setStyle("-fx-background-color: derive(-fx-accent, 100%);");
            } else {
                vBox.getChildren().get(i).setStyle("-fx-background-color: -fx-base;");
            }
        }
    }

    public void setOptionsListener(PredictiveTextFieldOptionsListener optionsListener) {
        this.optionsListener = optionsListener;
    }

    public PredictiveTextFieldOptionsListener getOptionsListener() {
        return optionsListener;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

}
