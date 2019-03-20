package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import menagerie.util.PokeListener;

import java.text.DecimalFormat;
import java.util.Formatter;

public class ProgressScreen extends Screen {

    private final Label title;
    private final Label message;
    private final Label count;
    private final ProgressBar progress;

    private PokeListener cancelListener = null;

    private static DecimalFormat df = new DecimalFormat("#.##");


    public ProgressScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: -fx-base;");
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        root.setPrefWidth(400);
        setCenter(root);

        title = new Label("No Title");
        BorderPane.setAlignment(title, Pos.CENTER_LEFT);
        root.setTop(title);

        progress = new ProgressBar();
        progress.setPrefWidth(root.getPrefWidth());
        message = new Label("No Message");
        VBox center = new VBox(5, message, progress);
        center.setPadding(new Insets(5));
        root.setCenter(center);

        BorderPane bottom = new BorderPane();
        setMargin(bottom, new Insets(5));
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> {
            close();
            event.consume();
        });
        bottom.setRight(cancel);
        count = new Label("0/0");
        count.setPadding(new Insets(5));
        bottom.setLeft(count);
        root.setBottom(bottom);

        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);

        setDefaultFocusNode(cancel);
    }

    public void open(ScreenPane manager, String titleText, String messageText, PokeListener cancelListener) {
        this.cancelListener = null;
        manager.open(this);

        this.cancelListener = cancelListener;

        title.setText(titleText);
        message.setText(messageText);
        setProgress(0, 0);
    }

    public void setProgress(int i, int total) {
        count.setText(df.format(100.0 * i / total) + "%");
        progress.setProgress((double) i / total);
    }

    @Override
    protected void onClose() {
        if (cancelListener != null) cancelListener.poke();
    }

}
