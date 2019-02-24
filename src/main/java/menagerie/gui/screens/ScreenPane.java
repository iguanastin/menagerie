package menagerie.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

public class ScreenPane extends StackPane {

    final private Map<Screen, Node> lastFocusMap = new HashMap<>();


    public ScreenPane() {
        setPickOnBounds(false);
    }

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
