package menagerie.gui.screens.importer;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.gui.screens.Screen;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.util.listeners.ObjectListener;

import java.util.ArrayList;
import java.util.List;

public class ImporterScreen extends Screen {

    private final Label countLabel;
    private final ListView<ImportJob> listView;

    private final List<ImportJob> jobs = new ArrayList<>();


    public ImporterScreen(ImporterThread importerThread, ObjectListener<List<SimilarPair<MediaItem>>> duplicateResolverListener, ObjectListener<MediaItem> selectItemListener) {

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.N && event.isControlDown()) {
                close();
                event.consume();
            }
        });


        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        Label title = new Label("Imports");
        setAlignment(title, Pos.CENTER_LEFT);
        BorderPane top = new BorderPane(null, null, exit, null, title);
        top.setPadding(new Insets(0, 0, 0, 5));

        listView = new ListView<>();
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.N && event.isControlDown()) {
                close();
                event.consume();
            }
        });

        Button playPauseButton = new Button("Pause");
        playPauseButton.setOnAction(event -> {
            importerThread.setPaused(!importerThread.isPaused());
            if (importerThread.isPaused()) {
                playPauseButton.setText("Resume");
            } else {
                playPauseButton.setText("Pause");
            }
        });
        countLabel = new Label("0");
        Button cancelAllButton = new Button("Cancel All");
        cancelAllButton.setOnAction(event -> {
            jobs.forEach(job -> {
                if (job.getStatus() == ImportJob.Status.WAITING) {
                    job.cancel();
                    listView.getItems().remove(job);
                }
            });
        });
        BorderPane bottom = new BorderPane(countLabel, null, playPauseButton, null, cancelAllButton);
        bottom.setPadding(new Insets(5));
        setAlignment(countLabel, Pos.CENTER);

        listView.getItems().addListener((ListChangeListener<? super ImportJob>) c -> countLabel.setText("" + c.getList().size()));

        BorderPane root = new BorderPane(listView, top, null, bottom, null);
        root.setPrefWidth(400);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setMaxWidth(USE_PREF_SIZE);
        setRight(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(playPauseButton);


        // ImporterThread setup
        listView.setCellFactory(param -> new ImportListCell(this, duplicateResolverListener, selectItemListener));
        importerThread.addImporterListener(job -> {
            jobs.add(job);
            listView.getItems().add(job);
            job.addStatusListener(status -> {
                if (status == ImportJob.Status.SUCCEEDED) {
                    removeJob(job);
                } else {
                    listView.getChildrenUnmodifiable().forEach(node -> {
                        if (node instanceof ImportListCell) {
                            ((ImportListCell) node).updateItem(((ImportListCell) node).getItem(), ((ImportListCell) node).isEmpty());
                        }
                    });
                }
            });
        });
    }

    /**
     * Removes a job from the list.
     *
     * @param job Job to remove.
     */
    void removeJob(ImportJob job) {
        jobs.remove(job);
        Platform.runLater(() -> listView.getItems().remove(job));
    }

    /**
     *
     * @return The list view of this import screen.
     */
    public ListView<ImportJob> getListView() {
        return listView;
    }

}
