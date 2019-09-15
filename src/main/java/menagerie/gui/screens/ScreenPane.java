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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A pane that manages stacked screens.
 */
public class ScreenPane extends StackPane {

    final private Map<Screen, Node> lastFocusMap = new HashMap<>();
    final private Stack<Screen> openStack = new Stack<>();
    final private ObjectProperty<Screen> current = new SimpleObjectProperty<>(null);


    public ScreenPane() {
        setPickOnBounds(false);
    }

    /**
     * Opens a screen on top of all other currently open screens, disabling everything that's not on top.
     *
     * @param screen Screen to open.
     */
    public boolean open(Screen screen) {
        if (!getChildren().contains(screen)) {
            screen.setManager(this);
            getChildren().add(screen);
        }

        if (current.get() != null) {
            lastFocusMap.put(current.get(), getScene().getFocusOwner());
            current.get().setDisable(true);
            openStack.push(current.get());
        }

        current.set(screen);

        screen.setDisable(false);
        screen.focusDefaultNode();

        screen.onOpen();
        return true;
    }

    /**
     * Closes a screen and updates the state of the top most screen to be enabled.
     *
     * @param screen Screen to close.
     * @return True if successful, false otherwise.
     */
    public boolean close(Screen screen) {
        if (getChildren().contains(screen)) {
            if (screen.equals(current.get())) {
                if (openStack.empty()) {
                    current.set(null);
                } else {
                    current.set(openStack.pop());
                    current.get().setDisable(false);
                    Node focusTarget = lastFocusMap.remove(current.get());
                    if (focusTarget != null) focusTarget.requestFocus();
                }
            } else {
                openStack.remove(screen);
            }

            screen.onClose();
            getChildren().remove(screen);
            screen.setManager(null);
            return true;
        } else {
            return false;
        }
    }

    public ObjectProperty<Screen> currentProperty() {
        return current;
    }

}
