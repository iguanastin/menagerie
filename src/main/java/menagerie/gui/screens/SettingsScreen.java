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
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import menagerie.gui.Main;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.model.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;

public class SettingsScreen extends Screen {

    private static final Insets ALL5 = new Insets(5);
    private static final Insets LEFT20 = new Insets(0, 0, 0, 20);
    private static final Font BOLD_ITALIC = new Font("System Bold Italic", 12);

    private final TextField defaultFolderTextField = new TextField();
    private final CheckBox autoImportFolderCheckBox = new CheckBox("Auto-import from folder");
    private final CheckBox autoImportMoveToDefaultCheckBox = new CheckBox("Move to default folder before importing");
    private final TextField importFolderTextField = new TextField();
    private final CheckBox fileNameFromURLCheckBox = new CheckBox("Automatically use filename from URL when importing from web");
    private final CheckBox tagWithTagmeCheckBox = new CheckBox("Tag imported items with 'tagme'");
    private final CheckBox tagWithVideoCheckBox = new CheckBox("Tag imported videos with 'video'");
    private final CheckBox tagWithImageCheckBox = new CheckBox("Tag imported images with 'image'");

    private final TextField confidenceTextField = new TextField();

    private final CheckBox muteVideoCheckBox;
    private final CheckBox repeatVideoCheckBox;

    private final ChoiceBox<Integer> gridWidthChoiceBox;

    private final TextField databaseURLTextField = new TextField();
    private final TextField databaseUserTextField = new TextField();
    private final TextField databasePasswordTextField = new TextField();
    private final CheckBox backupDatabaseCheckBox = new CheckBox("Backup database on launch");


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

        Label l = new Label("Settings");
        l.setPadding(ALL5);
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane top = new BorderPane(null, null, exit, new Separator(), l);

        // ----------------------- Center ------------------------------
        VBox rootV = new VBox(5);
        rootV.setPadding(ALL5);
        ScrollPane center = new ScrollPane(rootV);
        center.setFitToWidth(true);

        l = new Label("Importing");
        l.setFont(BOLD_ITALIC);
        VBox v = new VBox(5);
        v.setPadding(LEFT20);
        rootV.getChildren().addAll(l, v);
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
        v.getChildren().add(h);

        v.getChildren().add(autoImportFolderCheckBox);
        VBox v2 = new VBox(5, autoImportMoveToDefaultCheckBox);
        v2.setPadding(LEFT20);
        v.getChildren().add(v2);
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
        v2.disableProperty().bind(autoImportFolderCheckBox.selectedProperty().not());
        h = new HBox(5, new Label("Import from:"), importFolderTextField, browse);
        h.setAlignment(Pos.CENTER_LEFT);
        v2.getChildren().add(h);
        v.getChildren().add(fileNameFromURLCheckBox);
        v.getChildren().add(tagWithTagmeCheckBox);
        v.getChildren().add(tagWithVideoCheckBox);
        v.getChildren().add(tagWithImageCheckBox);

        l = new Label("Duplicate finding");
        l.setFont(BOLD_ITALIC);
        rootV.getChildren().addAll(new Separator(), l);
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
        h = new HBox(5, new Label("Confidence:"), confidenceTextField, new Label("(0.8-1.0)"));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(LEFT20);
        rootV.getChildren().add(h);

        l = new Label("Video viewing");
        l.setFont(BOLD_ITALIC);
        rootV.getChildren().addAll(new Separator(), l);
        muteVideoCheckBox = new CheckBox("Mute video preview");
        repeatVideoCheckBox = new CheckBox("Repeat video preview");
        v = new VBox(5, muteVideoCheckBox, repeatVideoCheckBox);
        v.setPadding(LEFT20);
        rootV.getChildren().add(v);

        l = new Label("Explorer Grid");
        l.setFont(BOLD_ITALIC);
        rootV.getChildren().addAll(new Separator(), l);
        gridWidthChoiceBox = new ChoiceBox<>();
        gridWidthChoiceBox.getItems().addAll(2, 3, 4, 5, 6);
        h = new HBox(5, new Label("Grid width:"), gridWidthChoiceBox);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(LEFT20);
        rootV.getChildren().add(h);

        l = new Label("Database (changes applied on restart)");
        l.setFont(BOLD_ITALIC);
        rootV.getChildren().addAll(new Separator(), l);
        HBox.setHgrow(databaseURLTextField, Priority.ALWAYS);
        HBox h1 = new HBox(5, new Label("URL:"), databaseURLTextField);
        h1.setAlignment(Pos.CENTER_LEFT);
        databaseUserTextField.setPromptText("None");
        HBox h2 = new HBox(5, new Label("User:"), databaseUserTextField);
        h2.setAlignment(Pos.CENTER_LEFT);
        databasePasswordTextField.setPromptText("None");
        HBox h3 = new HBox(5, new Label("Password:"), databasePasswordTextField);
        v = new VBox(5, h1, h2, h3, backupDatabaseCheckBox);
        v.setPadding(LEFT20);
        rootV.getChildren().add(v);

        // ------------------------- Bottom -----------------------------
        Button accept = new Button("Accept");
        accept.setOnAction(event -> {
            final boolean defaultFolderEmpty = defaultFolderTextField.getText() == null || defaultFolderTextField.getText().isEmpty();

            if (defaultFolderEmpty && autoImportMoveToDefaultCheckBox.isSelected()) {
                new AlertDialogScreen().open(getManager(), "Error", "Cannot move to default after auto-import if default folder is not specified.", null);
            } else if (defaultFolderEmpty && fileNameFromURLCheckBox.isSelected()) {
                new AlertDialogScreen().open(getManager(), "Error", "Cannot use filename from web if default folder is not specified.", null);
            } else {
                applyToSettings();
                close();
            }
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, accept, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        // -------------------------- Root --------------------------------
        BorderPane root = new BorderPane(center, top, null, bottom, null);
        root.setStyle("-fx-background-color: -fx-base;");
        root.setPrefSize(700, 600);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);

        setPadding(new Insets(25));
        setCenter(root);

        setDefaultFocusNode(cancel);
    }

    /**
     * Opens this screen in a manager.
     *
     * @param manager Manager.
     */
    public void open(ScreenPane manager) {
        manager.open(this);
    }

    /**
     * Applies inputs to settings object and saves it.
     */
    private void applyToSettings() {
        settings.setString(Settings.Key.DEFAULT_FOLDER, defaultFolderTextField.getText());
        settings.setBoolean(Settings.Key.DO_AUTO_IMPORT, autoImportFolderCheckBox.isSelected());
        settings.setBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT, autoImportMoveToDefaultCheckBox.isSelected());
        settings.setString(Settings.Key.AUTO_IMPORT_FOLDER, importFolderTextField.getText());
        settings.setBoolean(Settings.Key.USE_FILENAME_FROM_URL, fileNameFromURLCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_TAGME, tagWithTagmeCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_VIDEO, tagWithVideoCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_IMAGE, tagWithImageCheckBox.isSelected());
        double confidence = 0.95;
        try {
            confidence = Double.parseDouble(confidenceTextField.getText());
            if (confidence < 0.8) confidence = 0.8;
            else if (confidence > 1) confidence = 1;
        } catch (NumberFormatException ignored) {
        }
        settings.setDouble(Settings.Key.CONFIDENCE, confidence);
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
            Main.log.log(Level.WARNING, "Unable to save settings file", e);
        }
    }

    @Override
    protected void onOpen() {
        defaultFolderTextField.setText(settings.getString(Settings.Key.DEFAULT_FOLDER));
        autoImportFolderCheckBox.setSelected(settings.getBoolean(Settings.Key.DO_AUTO_IMPORT));
        autoImportMoveToDefaultCheckBox.setSelected(settings.getBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT));
        importFolderTextField.setText(settings.getString(Settings.Key.AUTO_IMPORT_FOLDER));
        fileNameFromURLCheckBox.setSelected(settings.getBoolean(Settings.Key.USE_FILENAME_FROM_URL));
        tagWithTagmeCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_TAGME));
        tagWithVideoCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_VIDEO));
        tagWithImageCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_IMAGE));
        double confidence = settings.getDouble(Settings.Key.CONFIDENCE);
        if (confidence < 0.8) confidence = 0.8;
        else if (confidence > 1) confidence = 1;
        confidenceTextField.setText("" + confidence);
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
