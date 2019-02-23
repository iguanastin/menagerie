package menagerie.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class Screen extends BorderPane {

    private ScreenPane manager = null;

    private Node defaultFocusNode = this;


    public void close() {
        if (manager == null) return;

        manager.close(this);
    }

    public void setManager(ScreenPane manager) {
        this.manager = manager;
    }

    public ScreenPane getManager() {
        return manager;
    }

    public void setDefaultFocusNode(Node defaultFocusNode) {
        this.defaultFocusNode = defaultFocusNode;
    }

    public void focusDefaultNode() {
        defaultFocusNode.requestFocus();
    }

    protected void onShow() {}

    protected void onHide() {}

}
