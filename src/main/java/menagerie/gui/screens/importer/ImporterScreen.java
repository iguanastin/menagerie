package menagerie.gui.screens.importer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.util.listeners.ObjectListener;

import java.util.ArrayList;
import java.util.List;

public class ImporterScreen extends Screen {

    private final ListView<ImportJob> listView = new ListView<>();

    private final List<ImportJob> jobs = new ArrayList<>();

    private final ObservableList<SimilarPair<MediaItem>> similar = FXCollections.observableArrayList();


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
        Label title = new Label("Imports: 0");
        setAlignment(title, Pos.CENTER_LEFT);
        BorderPane top = new BorderPane(null, null, exit, null, title);
        top.setPadding(new Insets(0, 0, 0, 5));

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
        Button cancelAllButton = new Button("Cancel All");
        cancelAllButton.setOnAction(event -> {
            jobs.forEach(job -> {
                if (job.getStatus() == ImportJob.Status.WAITING) {
                    job.cancel();
                    listView.getItems().remove(job);
                }
            });
        });
        Button pairsButton = new Button("Similar: 0");
        similar.addListener((ListChangeListener<? super SimilarPair<MediaItem>>) c -> Platform.runLater(() -> {
            pairsButton.setText("Similar: " + c.getList().size());
            if (c.getList().isEmpty()) {
                setStyle(null);
            } else {
                setStyle("-fx-color: blue;");
            }
        }));
        pairsButton.setOnAction(event -> {
            //TODO
        });
        BorderPane bottom = new BorderPane(pairsButton, null, playPauseButton, null, cancelAllButton);
        bottom.setPadding(new Insets(5));

        listView.getItems().addListener((ListChangeListener<? super ImportJob>) c -> title.setText("Imports: " + c.getList().size()));

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
        listView.setCellFactory(param -> new ImportListCell(this, selectItemListener));
        importerThread.addImporterListener(job -> {
            jobs.add(job);
            listView.getItems().add(job);
            job.addStatusListener(status -> {
                if (status == ImportJob.Status.SUCCEEDED) {
                    if (job.getSimilarTo() != null && !job.getSimilarTo().isEmpty()) {
                        job.getSimilarTo().forEach(pair -> {
                            if (similar.contains(pair)) similar.add(pair);
                        });
                    }
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

    /**
     *
     * @return List of similar pairs of imports.
     */
    public ObservableList<SimilarPair<MediaItem>> getSimilar() {
        return similar;
    }

}
