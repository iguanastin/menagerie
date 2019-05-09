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

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * A pane that manages stacked screens.
 */
public class ScreenPane extends StackPane {

    final private Map<Screen, Node> lastFocusMap = new HashMap<>();


    public ScreenPane() {
        setPickOnBounds(false);
    }

    /**
     * Opens a screen on top of all other currently open screens, disabling everything that's not on top.
     *
     * @param screen Screen to open.
     */
    public void open(Screen screen) {
        if (getChildren().contains(screen)) return;

        if (!getChildren().isEmpty()) {
            Screen oldScreen = (Screen) getChildren().get(getChildren().size() - 1);
            lastFocusMap.put(oldScreen, getScene().getFocusOwner());
            oldScreen.setDisable(true);
        }

        getChildren().add(screen);
        screen.setManager(this);
        screen.setDisable(false);
        screen.focusDefaultNode();

        screen.onOpen();
    }

    /**
     * Closes a screen and updates the state of the top most screen to be enabled.
     *
     * @param screen Screen to close.
     * @return True if successful, false otherwise.
     */
    public boolean close(Screen screen) {
        if (getChildren().remove(screen)) {
            screen.setManager(null);

            if (!getChildren().isEmpty()) {
                Screen prevScreen = (Screen) getChildren().get(getChildren().size() - 1);
                prevScreen.setDisable(false);
                Node focusTarget = lastFocusMap.remove(prevScreen);
                if (focusTarget != null) focusTarget.requestFocus();
            }

            screen.onClose();
            return true;
        } else {
            return false;
        }
    }

}
