package menagerie.gui.screens.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.PokeListener;

public class IntegerDialogScreen extends Screen {

    private final Spinner<Integer> spinner = new Spinner<>();
    private final Label titleLabel = new Label("N/A");
    private final Label messageLabel = new Label("N/A");

    private PokeListener cancelListener = null;
    private IntegerDialogConfirmListener confirmListener = null;


    public IntegerDialogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancel();
            } else if (event.getCode() == KeyCode.ENTER) {
                confirm();
            }
        });


        // Top
        Button exit = new Button("X");
        exit.setOnAction(event -> cancel());
        titleLabel.setPadding(new Insets(5));
        BorderPane top = new BorderPane(null, null, exit, new Separator(), titleLabel);

        // Center
        VBox center = new VBox(5, messageLabel, spinner);
        center.setPadding(new Insets(5));

        // Bottom
        Button confirm = new Button("Confirm");
        confirm.setOnAction(event -> confirm());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> cancel());
        HBox bottom = new HBox(5, confirm, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        BorderPane root = new BorderPane(center, top, null, bottom, null);
        root.setPrefWidth(500);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(spinner);
    }

    /**
     * Confirms this dialog.
     */
    private void confirm() {
        close();
        if (confirmListener != null) confirmListener.confirmed(spinner.getValue());
    }

    /**
     * Cancels this dialog.
     */
    private void cancel() {
        close();
        if (cancelListener != null) cancelListener.poke();
    }

    /**
     * Opens this screen in a manager.
     *
     * @param manager         Manager to open in.
     * @param title           Title text.
     * @param message         Message text.
     * @param min             Min int value.
     * @param max             Max int value.
     * @param val             Default value.
     * @param confirmListener Listener waiting for confirm event.
     * @param closeListener   Listener waiting for cancel event.
     */
    public void open(ScreenPane manager, String title, String message, int min, int max, int val, IntegerDialogConfirmListener confirmListener, PokeListener closeListener) {
        manager.open(this);

        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, val));
        titleLabel.setText(title);
        messageLabel.setText(message);

        this.confirmListener = confirmListener;
        this.cancelListener = closeListener;
    }

}
