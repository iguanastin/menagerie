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

package menagerie.gui.taglist;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import menagerie.model.menagerie.Tag;

public class TagListCell extends ListCell<Tag> {

    private static final String DEFAULT_STYLE_CLASS = "tag-list-cell";

    private final Label countLabel = new Label();
    private final Label nameLabel = new Label();

    private final ChangeListener<String> colorListener = (observable, oldValue, newValue) -> {
        if (Platform.isFxApplicationThread()) {
            setTextColor(newValue);
        } else {
            Platform.runLater(() -> setTextColor(newValue));
        }
    };
    private final ChangeListener<Number> frequencyListener = (observable, oldValue, newValue) -> {
        if (Platform.isFxApplicationThread()) {
            setFrequency(newValue.intValue());
        } else {
            Platform.runLater(() -> setFrequency(newValue.intValue()));
        }
    };


    public TagListCell() {
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);

        countLabel.setMinWidth(USE_PREF_SIZE);
        BorderPane bp = new BorderPane(null, null, countLabel, null, nameLabel);
        setGraphic(bp);
        nameLabel.maxWidthProperty().bind(bp.widthProperty().subtract(countLabel.widthProperty()).subtract(15));
    }

    @Override
    protected void updateItem(Tag tag, boolean empty) {
        if (getItem() != null) {
            getItem().colorProperty().removeListener(colorListener);
            getItem().frequencyProperty().removeListener(frequencyListener);
        }

        super.updateItem(tag, empty);

        if (empty || tag == null) {
            nameLabel.setText(null);
            countLabel.setText(null);
            setTooltip(null);
            setTextColor(null);
        } else {
            nameLabel.setText(tag.getName());
            setFrequency(tag.getFrequency());
            setTooltip(new Tooltip("(ID: " + tag.getId() + ") " + tag.getName()));
            setTextColor(tag.getColor());

            tag.colorProperty().addListener(colorListener);
            tag.frequencyProperty().addListener(frequencyListener);
        }
    }

    private void setFrequency(int freq) {
        countLabel.setText("(" + freq + ")");
    }

    private void setTextColor(String color) {
        if (color == null || color.isEmpty()) {
            nameLabel.setTextFill(Color.WHITE);
            countLabel.setTextFill(Color.WHITE);
        } else {
            nameLabel.setTextFill(Paint.valueOf(color));
            countLabel.setTextFill(Paint.valueOf(color));
        }
    }

}
