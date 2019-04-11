package menagerie.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Screen object that can be opened and managed in the ScreenPane.
 */
public abstract class Screen extends BorderPane {

    private ScreenPane manager = null;

    private Node defaultFocusNode = this;


    /**
     * Closes this screen if it is open in a manager.
     */
    public void close() {
        if (manager == null) return;

        manager.close(this);
    }

    /**
     * Sets the manager of this screen.
     *
     * @param manager Manager.
     */
    public void setManager(ScreenPane manager) {
        this.manager = manager;
    }

    /**
     * @return The manager of this screen, or null if the screen is not open.
     */
    public ScreenPane getManager() {
        return manager;
    }

    /**
     * @param defaultFocusNode Node to be focused on open.
     */
    public void setDefaultFocusNode(Node defaultFocusNode) {
        this.defaultFocusNode = defaultFocusNode;
    }

    /**
     * Attempts to focus the default focus node.
     */
    void focusDefaultNode() {
        defaultFocusNode.requestFocus();
    }

    /**
     * Method that is called after a screen has been opened.
     */
    protected void onOpen() {
    }

    /**
     * Method that is called after a screen has been closed.
     */
    protected void onClose() {
    }

}
