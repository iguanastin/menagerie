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
