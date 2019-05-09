package menagerie.gui.screens.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.listeners.PokeListener;

public class AlertDialogScreen extends Screen {

    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();

    private PokeListener closeListener = null;


    public AlertDialogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case ENTER:
                case ESCAPE:
                case SPACE:
                case BACK_SPACE:
                    close();
                    event.consume();
                    break;
            }
        });


        // Top
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        titleLabel.setPadding(new Insets(5));
        BorderPane top = new BorderPane(null, null, exit, new Separator(), titleLabel);

        // Center
        messageLabel.setPadding(new Insets(5));
        BorderPane.setAlignment(messageLabel, Pos.CENTER_LEFT);

        // Bottom
        Button ok = new Button("Ok");
        ok.setOnAction(event -> close());
        BorderPane.setAlignment(ok, Pos.BOTTOM_RIGHT);

        BorderPane root = new BorderPane(messageLabel, top, null, ok, null);
        root.setPrefWidth(500);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(ok);
    }

    public void open(ScreenPane manager, String title, String message, PokeListener closeListener) {
        this.closeListener = closeListener;

        titleLabel.setText(title);
        messageLabel.setText(message);

        manager.open(this);
    }

    @Override
    protected void onClose() {
        if (closeListener != null) closeListener.poke();
    }

}