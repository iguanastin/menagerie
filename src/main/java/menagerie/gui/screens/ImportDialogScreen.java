package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import menagerie.model.Settings;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.util.Filters;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ImportDialogScreen extends Screen {

    private final TextField filesTextField = new TextField();
    private final ChoiceBox<Order> orderChoiceBox = new ChoiceBox<>();
    private final CheckBox recursiveCheckBox = new CheckBox("Recursively import folders");
    private final CheckBox tagWithParentCheckBox = new CheckBox("Tag with parent folder name");
    private final CheckBox tagWithTagCheckBox = new CheckBox("Tag with specified tag:");
    private final TextField tagWithTagTextField = new TextField();
    private final ImporterThread importer;
    private final Menagerie menagerie;
    private List<File> files = new ArrayList<>();

    public ImportDialogScreen(Settings settings, Menagerie menagerie, ImporterThread importer) {
        this.menagerie = menagerie;
        this.importer = importer;

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });


        // ---------------------------------- Header ---------------------------------------
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, exit, new Separator(), new Label("Import local files and folders"));
        setPadding(new Insets(0, 0, 0, 5));

        // ---------------------------------- Center ---------------------------------------
        // Files option
        HBox.setHgrow(filesTextField, Priority.ALWAYS);
        Button browseFiles = new Button("Browse Files");
        browseFiles.setOnAction(event -> {
            FileChooser fc = new FileChooser();
            fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
            fc.setTitle("Import files...");
            if (settings.getString(Settings.Key.DEFAULT_FOLDER) != null) {
                File f = new File(settings.getString(Settings.Key.DEFAULT_FOLDER));
                if (f.isDirectory()) fc.setInitialDirectory(f);
            }
            List<File> result = fc.showOpenMultipleDialog(getScene().getWindow());
            if (result != null && !result.isEmpty()) {
                files = new ArrayList<>(result);
                if (files.size() > 1) {
                    filesTextField.setText(String.format("%d files", files.size()));
                } else {
                    filesTextField.setText(files.get(0).toString());
                }
            }
        });
        Button browseFolders = new Button("Browse Folders");
        browseFolders.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Import folder...");
            if (settings.getString(Settings.Key.DEFAULT_FOLDER) != null) {
                File f = new File(settings.getString(Settings.Key.DEFAULT_FOLDER));
                if (f.isDirectory()) dc.setInitialDirectory(f);
            }
            File folder = dc.showDialog(getScene().getWindow());
            if (folder != null) {
                files = new ArrayList<>();
                files.add(folder);
                filesTextField.setText(folder.toString());
            }
        });
        HBox fileHBox = new HBox(5, filesTextField, browseFiles, browseFolders);
        // Order option
        orderChoiceBox.getItems().addAll(Order.values());
        orderChoiceBox.getSelectionModel().selectFirst();
        HBox orderHBox = new HBox(5, new Label("Import files in order:"), orderChoiceBox);
        // Tag on import
        HBox tagHBox = new HBox(5, tagWithTagCheckBox, tagWithTagTextField);
        tagHBox.setAlignment(Pos.CENTER_LEFT);
        VBox center = new VBox(5, fileHBox, orderHBox, recursiveCheckBox, tagWithParentCheckBox, tagHBox);
        center.setPadding(new Insets(5));

        // ----------------------------------- Bottom --------------------------------------
        Button accept = new Button("Import");
        accept.setOnAction(event -> importOnAction());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, accept, cancel);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(5));

        // --------------------------------- Root ------------------------------------------
        BorderPane root = new BorderPane(center, header, null, bottom, null);
        root.setMaxSize(600, 250);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(cancel);
    }

    @Override
    protected void onOpen() {
        filesTextField.setText(null);
    }

    private void importOnAction() {
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            if (file.isDirectory()) {
                for (File f2 : Objects.requireNonNull(file.listFiles())) {
                    if (Filters.FILE_NAME_FILTER.accept(f2) || (recursiveCheckBox.isSelected() && f2.isDirectory())) {
                        files.add(f2);
                    }
                }
                files.remove(i);
                i--;
            }
        }

        switch (orderChoiceBox.getValue()) {
            case Date_Modified:
                files.sort(Comparator.comparingLong(File::lastModified));
                break;
            case Alphabetical:
                files.sort(Comparator.comparing(File::getName));
                break;
            case Default:
            default:
                break;
        }

        for (File file : files) {
            final ImportJob job = new ImportJob(file);
            final List<String> tagsToAdd = new ArrayList<>();

            if (tagWithParentCheckBox.isSelected()) tagsToAdd.add(file.getParentFile().getName().toLowerCase());
            if (tagWithTagCheckBox.isSelected() && tagWithTagTextField.getText() != null && !tagWithTagTextField.getText().isEmpty())
                tagsToAdd.add(tagWithTagTextField.getText().toLowerCase());

            if (!tagsToAdd.isEmpty()) {
                job.addStatusListener(status -> {
                    if (status == ImportJob.Status.SUCCEEDED || status == ImportJob.Status.SUCCEEDED_SIMILAR) {
                        for (String tagName : tagsToAdd) {
                            Tag t = menagerie.getTagByName(tagName);
                            if (t == null) t = menagerie.createTag(tagName);
                            job.getItem().addTag(t);
                        }
                    }
                });
            }

            importer.queue(job);
        }

        close();
    }

    private enum Order {
        Default, Alphabetical, Date_Modified
    }

}
