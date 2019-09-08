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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;

public class PredictivePopupCell extends Label {

    private static final String DEFAULT_STYLE_CLASS = "predictive-cell";

    private static final PseudoClass SELECTED_PSEUDOCLASS = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass SELECTED_UNFOCUSED_PSEUDOCLASS = PseudoClass.getPseudoClass("selected-unfocused");

    private final BooleanProperty selected = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(SELECTED_PSEUDOCLASS, get());
        }

        @Override
        public Object getBean() {
            return PredictivePopupCell.this;
        }

        @Override
        public String getName() {
            return "selected";
        }
    };

    private final BooleanProperty selectedUnfocused = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(SELECTED_UNFOCUSED_PSEUDOCLASS, get());
        }

        @Override
        public Object getBean() {
            return PredictivePopupCell.this;
        }

        @Override
        public String getName() {
            return "selected-unfocused";
        }
    };


    public PredictivePopupCell() {
        super();
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }

    public PredictivePopupCell(String text) {
        super(text);
        getStyleClass().addAll(DEFAULT_STYLE_CLASS);
    }

    public BooleanProperty selectedUnfocusedProperty() {
        return selectedUnfocused;
    }

    public boolean getSelectedUnfocused() {
        return selectedUnfocused.get();
    }

    public void setSelectedUnfocused(boolean b) {
        selectedUnfocused.set(b);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean b) {
        selected.set(b);
    }

}
