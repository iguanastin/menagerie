package menagerie.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class Screen extends BorderPane {

    protected Node lastFocusOwner = null;
    protected Node onShowFocus = this;
    protected Node onShowDisable;


    public Screen(Node onShowDisable) {
        this.onShowDisable = onShowDisable;
        setDisable(true);
        setOpacity(0);
    }


    public void show() {
        lastFocusOwner = getScene().getFocusOwner();
        setDisable(false);
        setOpacity(1);
        if (onShowDisable != null) onShowDisable.setDisable(true);
        if (onShowFocus != null) onShowFocus.requestFocus();
    }

    public void hide() {
        setDisable(true);
        setOpacity(0);
        if (onShowDisable != null) onShowDisable.setDisable(false);
        if (lastFocusOwner != null) lastFocusOwner.requestFocus();
    }

}
