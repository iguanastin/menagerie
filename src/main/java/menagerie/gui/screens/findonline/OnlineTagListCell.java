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

package menagerie.gui.screens.findonline;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;

public class OnlineTagListCell extends ListCell<String> {

    private static final String DEFAULT_STYLE_CLASS = "online-tag-cell";
    private static final PseudoClass SHARES_TAG = PseudoClass.getPseudoClass("shares_tag");

    private BooleanProperty sharesTag = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(SHARES_TAG, get());
        }

        @Override
        public Object getBean() {
            return OnlineTagListCell.this;
        }

        @Override
        public String getName() {
            return "shares_tag";
        }
    };

    private final OnlineTagCellCallback callback;


    public OnlineTagListCell(OnlineTagCellCallback callback) {
        super();
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
        this.callback = callback;
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        setText(item);
        if (callback != null) sharesTag.set(!empty && item != null && callback.hasTag(item));
    }

    public BooleanProperty sharesTagProperty() {
        return sharesTag;
    }

}
