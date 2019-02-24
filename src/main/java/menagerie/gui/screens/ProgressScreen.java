package menagerie.gui.screens;

import javafx.application.Platform;
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
import menagerie.gui.progress.ProgressLockThread;
import menagerie.gui.progress.ProgressLockThreadCancelListener;
import menagerie.gui.progress.ProgressLockThreadFinishListener;

import java.util.List;

public class ProgressScreen extends Screen {

    private ProgressLockThread progressThread;

    private final Label title;
    private final Label message;
    private final Label count;
    private final ProgressBar progress;


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

    public void open(ScreenPane manager, String titleText, String messageText, List<Runnable> tasks, ProgressLockThreadFinishListener finishListener, ProgressLockThreadCancelListener cancelListener) {
        manager.open(this);

        title.setText(titleText);
        message.setText(messageText);
        count.setText("0/" + tasks.size());
        progress.setProgress(0);

        if (progressThread != null) progressThread.stopRunning();
        progressThread = new ProgressLockThread(tasks);
        progressThread.setCancelListener((num, total) -> {
            Platform.runLater(this::close);
            if (cancelListener != null) cancelListener.progressCanceled(num, total);
        });
        progressThread.setFinishListener(total -> {
            Platform.runLater(this::close);
            if (finishListener != null) finishListener.progressFinished(total);
        });
        progressThread.setUpdateListener((num, total) -> Platform.runLater(() -> {
            final double p = (double) num / total;
            progress.setProgress(p);
            count.setText((int) (p * 100) + "% - " + (total - num) + " remaining...");
        }));
        progressThread.start();
    }

    @Override
    public void onClose() {
        if (progressThread != null) progressThread.stopRunning();
    }

}
