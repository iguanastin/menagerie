package menagerie.gui.screens.importer;

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
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;

import java.util.Iterator;

public class ImporterScreen extends Screen {

    private final Label countLabel;
    private final ListView<ImportJob> listView;


    public ImporterScreen(ImporterThread importerThread, ImporterCellDuplicateListener duplicateResolverListener, ImporterCellSelectItemListener selectItemListener) {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });


        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        Label title = new Label("Imports");
        setAlignment(title, Pos.CENTER_LEFT);
        BorderPane top = new BorderPane(null, null, exit, null, title);
        top.setPadding(new Insets(0, 0, 0, 5));

        listView = new ListView<>();

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
            Iterator<ImportJob> iter = listView.getItems().iterator();
            while (iter.hasNext()) {
                ImportJob job = iter.next();
                if (job.getStatus() == ImportJob.Status.WAITING) {
                    job.cancel();
                    iter.remove();
                }
            }
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
        listView.setCellFactory(param -> new ImportListCell(duplicateResolverListener, selectItemListener));
        importerThread.addImporterListener(job -> listView.getItems().add(job));
    }

    public ListView<ImportJob> getListView() {
        return listView;
    }

}
