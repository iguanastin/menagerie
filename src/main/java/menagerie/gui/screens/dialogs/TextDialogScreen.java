package menagerie.gui.screens.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.PokeListener;

public class TextDialogScreen extends Screen {

    private final Label titleLabel = new Label("N/A");
    private final Label messageLabel = new Label("N/A");
    private final TextField textField = new TextField();

    private TextDialogConfirmListener confirmListener;
    private PokeListener cancelListener;


    public TextDialogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                confirm();
            }
        });

        // --------------------------------- Header --------------------------------------
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane top = new BorderPane(null, null, exit, new Separator(), titleLabel);

        // --------------------------------- Center --------------------------------------
        VBox center = new VBox(5, messageLabel, textField);
        center.setPadding(new Insets(5));

        // --------------------------------- Bottom --------------------------------------
        Button confirm = new Button("Confirm");
        confirm.setOnAction(event -> confirm());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, confirm, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        // -------------------------------- Root -----------------------------------------
        BorderPane root = new BorderPane(center, top, null, bottom, null);
        root.setPrefWidth(500);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(textField);
    }

    public void open(ScreenPane manager, String title, String message, String text, TextDialogConfirmListener confirmListener, PokeListener cancelListener) {
        manager.open(this);

        titleLabel.setText(title);
        messageLabel.setText(message);
        textField.setText(text);
        textField.selectAll();
        this.confirmListener = confirmListener;
        this.cancelListener = cancelListener;
    }

    private void confirm() {
        super.close();
        if (confirmListener != null) confirmListener.confirmed(textField.getText());
    }

    @Override
    public void close() {
        super.close();
        if (cancelListener != null) cancelListener.poke();
    }

}