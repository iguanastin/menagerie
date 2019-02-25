package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.util.SimplePokeListener;

public class ConfirmationScreen extends Screen {

    private final Label titleLabel;
    private final Label messageLabel;

    private SimplePokeListener okListener = null;
    private SimplePokeListener cancelListener = null;


    public ConfirmationScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                if (okListener != null) okListener.poke();
                close();
                event.consume();
            }
        });

        titleLabel = new Label("TITLE");
        messageLabel = new Label("MESSAGE");
        messageLabel.setWrapText(true);

        Button ok = new Button("Ok");
        ok.setOnAction(event -> {
            if (okListener != null) okListener.poke();
            close();
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> {
            if (cancelListener != null) cancelListener.poke();
            close();
        });

        HBox h = new HBox(5, ok, cancel);
        h.setAlignment(Pos.CENTER_RIGHT);
        VBox v = new VBox(5, titleLabel, new Separator(), messageLabel, h);
        v.setPrefWidth(500);
        v.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        v.setPadding(new Insets(5));
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        v.setEffect(effect);
        v.setStyle("-fx-background-color: -fx-base;");
        setCenter(v);

        setDefaultFocusNode(ok);
    }

    public void open(ScreenPane manager, String title, String message, SimplePokeListener okListener, SimplePokeListener cancelListener) {
        manager.open(this);

        titleLabel.setText(title);
        messageLabel.setText(message);
        this.okListener = okListener;
        this.cancelListener = cancelListener;
    }

}
