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
import menagerie.model.Settings;

import java.io.File;
import java.io.FileNotFoundException;

public class SettingsScreen extends Screen {

    private final TextField defaultFolderTextField;

    private final CheckBox autoImportFolderCheckBox;
    private final CheckBox autoImportMoveToDefaultCheckBox;

    private final TextField importFolderTextField;

    private final CheckBox fileNameFromURLCheckBox;

    private final TextField confidenceTextField;
    private final CheckBox combineTagsCheckBox;
    private final CheckBox compareGreyscalesCheckBox;

    private final CheckBox muteVideoCheckBox;
    private final CheckBox repeatVideoCheckBox;

    private final ChoiceBox<Integer> gridWidthChoiceBox;

    private final TextField databaseURLTextField;
    private final TextField databaseUserTextField;
    private final TextField databasePasswordTextField;
    private final CheckBox backupDatabaseCheckBox;


    private final Settings settings;


    public SettingsScreen(Settings settings) {
        this.settings = settings;

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE || (event.getCode() == KeyCode.S && event.isControlDown())) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                applyToSettings();
                close();
            }
        });

        // ----------------------- Center ------------------------------
        VBox rootV = new VBox(5);
        rootV.setPadding(new Insets(5));
        ScrollPane center = new ScrollPane(rootV);
        center.setFitToWidth(true);
        rootV.getChildren().add(new Label("Settings:"));
        rootV.getChildren().add(new Separator());
        defaultFolderTextField = new TextField();
        defaultFolderTextField.setEditable(false);
        defaultFolderTextField.setPromptText("Folder will be asked for when saving");
        HBox.setHgrow(defaultFolderTextField, Priority.ALWAYS);
        Button browse = new Button("Browse");
        browse.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            if (defaultFolderTextField.getText() != null && !defaultFolderTextField.getText().isEmpty()) dc.setInitialDirectory(new File(defaultFolderTextField.getText()));
            dc.setTitle("Select default folder to import to");
            File result = dc.showDialog(getScene().getWindow());

            if (result != null) {
                defaultFolderTextField.setText(result.getAbsolutePath());
            }
        });
        HBox h = new HBox(5, new Label("Default Folder:"), defaultFolderTextField, browse);
        h.setAlignment(Pos.CENTER_LEFT);
        rootV.getChildren().add(h);
        rootV.getChildren().add(new Separator());
        autoImportFolderCheckBox = new CheckBox("Auto-import from folder");
        autoImportMoveToDefaultCheckBox = new CheckBox("Move from folder to default folder before importing");
        h = new HBox(5, autoImportFolderCheckBox, autoImportMoveToDefaultCheckBox);
        h.setAlignment(Pos.CENTER_LEFT);
        rootV.getChildren().add(h);
        importFolderTextField = new TextField();
        importFolderTextField.setPromptText("Folder to auto-import images from");
        importFolderTextField.setEditable(false);
        HBox.setHgrow(importFolderTextField, Priority.ALWAYS);
        browse = new Button("Browse");
        browse.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            if (importFolderTextField.getText() != null && !importFolderTextField.getText().isEmpty()) dc.setInitialDirectory(new File(importFolderTextField.getText()));
            dc.setTitle("Select folder to auto-import from");
            File result = dc.showDialog(getScene().getWindow());

            if (result != null) {
                importFolderTextField.setText(result.getAbsolutePath());
            }
        });
        autoImportMoveToDefaultCheckBox.disableProperty().bind(autoImportFolderCheckBox.selectedProperty().not());
        importFolderTextField.disableProperty().bind(autoImportFolderCheckBox.selectedProperty().not());
        browse.disableProperty().bind(autoImportFolderCheckBox.selectedProperty().not());
        h = new HBox(5, new Label("Import from:"), importFolderTextField, browse);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 0, 25));
        rootV.getChildren().add(h);
        fileNameFromURLCheckBox = new CheckBox("Automatically use filename from URL when importing from web");
        rootV.getChildren().add(fileNameFromURLCheckBox);
        rootV.getChildren().add(new Separator());
        rootV.getChildren().add(new Label("Duplicate finding:"));
        confidenceTextField = new TextField();
        confidenceTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double value = Double.parseDouble(confidenceTextField.getText());
                    if (value < 0.8) confidenceTextField.setText("0.8");
                    else if (value > 1) confidenceTextField.setText("1");
                } catch (NumberFormatException e) {
                    confidenceTextField.setText("0.95");
                }
            }
        });
        confidenceTextField.setPromptText("0.95 recommended");
        combineTagsCheckBox = new CheckBox("Combine tags when deleting a duplicate");
        compareGreyscalesCheckBox = new CheckBox("Compare greyscale images (poor accuracy)");
        h = new HBox(5, new Label("Similarity confidence:"), confidenceTextField, new Label("(0.8-1.0)"));
        h.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(h, combineTagsCheckBox, compareGreyscalesCheckBox);
        v.setPadding(new Insets(0, 0, 0, 25));
        rootV.getChildren().add(v);
        rootV.getChildren().add(new Separator());
        rootV.getChildren().add(new Label("Video:"));
        muteVideoCheckBox = new CheckBox("Mute video preview");
        repeatVideoCheckBox = new CheckBox("Repeat video preview");
        v = new VBox(5, muteVideoCheckBox, repeatVideoCheckBox);
        v.setPadding(new Insets(0, 0, 0, 25));
        rootV.getChildren().add(v);
        rootV.getChildren().add(new Separator());
        gridWidthChoiceBox = new ChoiceBox<>();
        gridWidthChoiceBox.getItems().addAll(2, 3, 4, 5, 6);
        h = new HBox(5, new Label("Grid width:"), gridWidthChoiceBox);
        h.setAlignment(Pos.CENTER_LEFT);
        rootV.getChildren().add(h);
        rootV.getChildren().add(new Separator());
        rootV.getChildren().add(new Label("Database (changes will be applied on restart):"));
        databaseURLTextField = new TextField();
        HBox.setHgrow(databaseURLTextField, Priority.ALWAYS);
        HBox h1 = new HBox(5, new Label("URL:"), databaseURLTextField);
        h1.setAlignment(Pos.CENTER_LEFT);
        databaseUserTextField = new TextField();
        HBox.setHgrow(databaseUserTextField, Priority.ALWAYS);
        HBox h2 = new HBox(5, new Label("User:"), databaseUserTextField);
        h2.setAlignment(Pos.CENTER_LEFT);
        databasePasswordTextField = new TextField();
        HBox.setHgrow(databasePasswordTextField, Priority.ALWAYS);
        HBox h3 = new HBox(5, new Label("Password:"), databasePasswordTextField);
        backupDatabaseCheckBox = new CheckBox("Backup database on launch");
        v = new VBox(5, h1, h2, h3, backupDatabaseCheckBox);
        v.setPadding(new Insets(0, 0, 0, 25));
        rootV.getChildren().add(v);

        // ------------------------- Bottom -----------------------------
        Button accept = new Button("Accept");
        accept.setOnAction(event -> {
            applyToSettings();
            close();
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, accept, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        // -------------------------- Root --------------------------------
        BorderPane root = new BorderPane(center, null, null, bottom, null);
        root.setStyle("-fx-background-color: -fx-base;");
        root.setPrefSize(700, 600);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);

        setPadding(new Insets(25));
        setCenter(root);

        setDefaultFocusNode(root);
    }

    public void open(ScreenPane manager) {
        manager.open(this);
    }

    private void applyToSettings() {
        settings.setString(Settings.Key.DEFAULT_FOLDER, defaultFolderTextField.getText());
        settings.setBoolean(Settings.Key.DO_AUTO_IMPORT, autoImportFolderCheckBox.isSelected());
        settings.setBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT, autoImportMoveToDefaultCheckBox.isSelected());
        settings.setString(Settings.Key.AUTO_IMPORT_FOLDER, importFolderTextField.getText());
        settings.setBoolean(Settings.Key.USE_FILENAME_FROM_URL, fileNameFromURLCheckBox.isSelected());
        double confidence = 0.95;
        try {
            confidence = Double.parseDouble(confidenceTextField.getText());
            if (confidence < 0.8) confidence = 0.8;
            else if (confidence > 1) confidence = 1;
        } catch (NumberFormatException ignored) {
        }
        settings.setDouble(Settings.Key.CONFIDENCE, confidence);
        settings.setBoolean(Settings.Key.COMBINE_TAGS, combineTagsCheckBox.isSelected());
        settings.setBoolean(Settings.Key.COMPARE_GREYSCALE, compareGreyscalesCheckBox.isSelected());
        settings.setBoolean(Settings.Key.MUTE_VIDEO, muteVideoCheckBox.isSelected());
        settings.setBoolean(Settings.Key.REPEAT_VIDEO, repeatVideoCheckBox.isSelected());
        int gridWidth = 2;
        if (gridWidthChoiceBox.getSelectionModel().getSelectedItem() != null) {
            gridWidth = gridWidthChoiceBox.getSelectionModel().getSelectedItem();
        }
        settings.setInt(Settings.Key.GRID_WIDTH, gridWidth);
        settings.setString(Settings.Key.DATABASE_URL, databaseURLTextField.getText());
        settings.setString(Settings.Key.DATABASE_USER, databaseUserTextField.getText());
        settings.setString(Settings.Key.DATABASE_PASSWORD, databasePasswordTextField.getText());
        settings.setBoolean(Settings.Key.BACKUP_DATABASE, backupDatabaseCheckBox.isSelected());

        try {
            settings.save();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onOpen() {
        defaultFolderTextField.setText(settings.getString(Settings.Key.DEFAULT_FOLDER));
        autoImportFolderCheckBox.setSelected(settings.getBoolean(Settings.Key.DO_AUTO_IMPORT));
        autoImportMoveToDefaultCheckBox.setSelected(settings.getBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT));
        importFolderTextField.setText(settings.getString(Settings.Key.AUTO_IMPORT_FOLDER));
        fileNameFromURLCheckBox.setSelected(settings.getBoolean(Settings.Key.USE_FILENAME_FROM_URL));
        double confidence = settings.getDouble(Settings.Key.CONFIDENCE);
        if (confidence < 0.8) confidence = 0.8;
        else if (confidence > 1) confidence = 1;
        confidenceTextField.setText("" + confidence);
        combineTagsCheckBox.setSelected(settings.getBoolean(Settings.Key.COMBINE_TAGS));
        compareGreyscalesCheckBox.setSelected(settings.getBoolean(Settings.Key.COMPARE_GREYSCALE));
        muteVideoCheckBox.setSelected(settings.getBoolean(Settings.Key.MUTE_VIDEO));
        repeatVideoCheckBox.setSelected(settings.getBoolean(Settings.Key.REPEAT_VIDEO));
        int gridWidth = settings.getInt(Settings.Key.GRID_WIDTH);
        if (gridWidth < 2) gridWidth = 2;
        else if (gridWidth > 6) gridWidth = 6;
        gridWidthChoiceBox.getSelectionModel().select((Integer) gridWidth);
        databaseURLTextField.setText(settings.getString(Settings.Key.DATABASE_URL));
        databaseUserTextField.setText(settings.getString(Settings.Key.DATABASE_USER));
        databasePasswordTextField.setText(settings.getString(Settings.Key.DATABASE_PASSWORD));
        backupDatabaseCheckBox.setSelected(settings.getBoolean(Settings.Key.BACKUP_DATABASE));
    }

}
