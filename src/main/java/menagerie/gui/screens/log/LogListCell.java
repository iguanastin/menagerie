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

package menagerie.gui.screens.log;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.logging.Level;

public class LogListCell extends ListCell<LogItem> {

    private final PseudoClass warningPseudoClass = PseudoClass.getPseudoClass("warning");
    private final PseudoClass errorPseudoClass = PseudoClass.getPseudoClass("error");

    private final Label label = new Label();

    private final BooleanProperty warning = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(warningPseudoClass, get());
        }

        @Override
        public Object getBean() {
            return LogListCell.class;
        }

        @Override
        public String getName() {
            return "warning";
        }
    };
    private final BooleanProperty error = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(errorPseudoClass, get());
        }

        @Override
        public Object getBean() {
            return LogListCell.class;
        }

        @Override
        public String getName() {
            return "error";
        }
    };


    public LogListCell() {
        super();
        getStyleClass().addAll("log-list-cell");

        label.setWrapText(true);
        label.maxWidthProperty().bind(widthProperty().subtract(15));
        setGraphic(label);

        setOnContextMenuRequested(event -> {
            if (label.getText() != null) {
                MenuItem copy = new MenuItem("Copy");
                copy.setOnAction(event1 -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(label.getText());
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                new ContextMenu(copy).show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
            }
        });
    }

    @Override
    protected void updateItem(LogItem item, boolean empty) {
        super.updateItem(item, empty);

        warning.set(item != null && item.getLevel() == Level.WARNING);
        error.set(item != null && item.getLevel() == Level.SEVERE);
        label.setText(item != null ? item.getText() : null);
    }
}
