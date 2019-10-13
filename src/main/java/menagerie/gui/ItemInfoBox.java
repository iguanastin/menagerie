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

package menagerie.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

public class ItemInfoBox extends VBox {

    private static final String DEFAULT_STYLE_CLASS = "item-info-box";

    private static final String DEFAULT_ID_TEXT = "ID: N/A";
    private static final String DEFAULT_DATE_TEXT = "N/A";
    private static final String DEFAULT_FILESIZE_TEXT = "0B";
    private static final String DEFAULT_RESOLUTION_TEXT = "0x0";
    private static final String DEFAULT_FILEPATH_TEXT = "N/A";

    private final Label idLabel = new Label(DEFAULT_ID_TEXT);
    private final Label dateLabel = new Label(DEFAULT_DATE_TEXT);
    private final Label fileSizeLabel = new Label(DEFAULT_FILESIZE_TEXT);
    private final Label resolutionLabel = new Label(DEFAULT_RESOLUTION_TEXT);
    private final Label filePathLabel = new Label(DEFAULT_FILEPATH_TEXT);

    /**
     * Extended state of this info box.
     */
    private BooleanProperty extended = new SimpleBooleanProperty(false);


    public ItemInfoBox() {
        setPadding(new Insets(5));
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        setSpacing(2);

        // Invert extended state on click
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> setExtended(!isExtended()));
        addEventHandler(MouseEvent.MOUSE_ENTERED, event -> getScene().setCursor(Cursor.HAND));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> getScene().setCursor(Cursor.DEFAULT));

        getChildren().addAll(resolutionLabel, fileSizeLabel);
    }

    /**
     * @return The extended state of this info box.
     */
    public boolean isExtended() {
        return extended.get();
    }

    /**
     * Changes the extended state of this info box and updates the GUI to reflect the change. When extended, shows all available data. Otherwise shows file size and resolution.
     *
     * @param b New extended state.
     */
    public void setExtended(boolean b) {
        if (b == extended.get()) return;

        extended.set(b);
        if (b) {
            getChildren().addAll(idLabel, dateLabel, filePathLabel);
        } else {
            getChildren().removeAll(idLabel, dateLabel, filePathLabel);
        }
    }

    /**
     * @return The extended state property.
     */
    public BooleanProperty extendedProperty() {
        return extended;
    }

    /**
     * Updates the info text and GUI.
     *
     * @param item Item to pull info from. If null, uses default text.
     */
    public void setItem(Item item) {
        idLabel.setText(DEFAULT_ID_TEXT);
        dateLabel.setText(DEFAULT_DATE_TEXT);
        fileSizeLabel.setText(DEFAULT_FILESIZE_TEXT);
        resolutionLabel.setText(DEFAULT_RESOLUTION_TEXT);
        filePathLabel.setText(DEFAULT_FILEPATH_TEXT);

        if (item != null) {
            idLabel.setText("ID: " + item.getId());
            dateLabel.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(new Date(item.getDateAdded()).toInstant()));
        }

        if (item instanceof MediaItem) {
            fileSizeLabel.setText(Util.bytesToPrettyString(((MediaItem) item).getFile().length()));
            filePathLabel.setText(((MediaItem) item).getFile().toString());
            if (((MediaItem) item).isImage()) { //TODO: Support for video resolution (May be possible in latest VLCJ api)
                if (((MediaItem) item).getImage().isBackgroundLoading() && ((MediaItem) item).getImage().getProgress() != 1) {
                    resolutionLabel.setText("Loading...");
                    ((MediaItem) item).getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue.doubleValue() == 1 && !((MediaItem) item).getImage().isError()) resolutionLabel.setText((int) ((MediaItem) item).getImage().getWidth() + "x" + (int) ((MediaItem) item).getImage().getHeight());
                    });
                } else {
                    resolutionLabel.setText((int) ((MediaItem) item).getImage().getWidth() + "x" + (int) ((MediaItem) item).getImage().getHeight());
                }
            }
        }
    }

}
