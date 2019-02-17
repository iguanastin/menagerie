package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ConfirmationScreen extends Screen {

    private Label titleLabel;
    private Label messageLabel;

    private ConfirmationScreenOkListener okListener = null;
    private ConfirmationScreenCancelListener cancelListener = null;


    public ConfirmationScreen(Node onShowDisable) {
        super(onShowDisable);

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
                event.consume();
            }
        });

        titleLabel = new Label("TITLE");
        messageLabel = new Label("MESSAGE");

        Button ok = new Button("Ok");
        ok.setOnAction(event -> {
            if (okListener != null) okListener.okayed();
            hide();
        });
        ok.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (okListener != null) okListener.okayed();
                hide();
                event.consume();
            }
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> {
            if (cancelListener != null) cancelListener.canceled();
            hide();
        });
        cancel.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (cancelListener != null) cancelListener.canceled();
                hide();
                event.consume();
            }
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

        onShowFocus = ok;
    }

    public void show(String title, String message, ConfirmationScreenOkListener okListener, ConfirmationScreenCancelListener cancelListener) {
        titleLabel.setText(title);
        messageLabel.setText(message);
        this.okListener = okListener;
        this.cancelListener = cancelListener;
        show();
    }

}
