/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

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

import static menagerie.model.menagerie.MediaItem.MAX_CONFIDENCE;
import static menagerie.model.menagerie.MediaItem.MIN_CONFIDENCE;

public class SettingsScreen extends Screen {

    private static final Insets ALL5 = new Insets(5);
    private static final Insets LEFT20 = new Insets(0, 0, 0, 20);
    private static final Font BOLD_ITALIC = new Font("System Bold Italic", 12);

    private static final double DEFAULT_CONFIDENCE = 0.95;

    private final TextField defaultFolderTextField = new TextField();
    private final CheckBox autoImportFolderCheckBox = new CheckBox("Auto-import from folder");
    private final CheckBox autoImportMoveToDefaultCheckBox = new CheckBox("Move to default folder before importing");
    private final TextField importFolderTextField = new TextField();
    private final TextField alsoImportExtensionsTextField = new TextField();
    private final CheckBox fileNameFromURLCheckBox = new CheckBox("Automatically use filename from URL when importing from web");
    private final CheckBox tagWithTagmeCheckBox = new CheckBox("Tag imported items with 'tagme'");
    private final CheckBox tagWithVideoCheckBox = new CheckBox("Tag imported videos with 'video'");
    private final CheckBox tagWithImageCheckBox = new CheckBox("Tag imported images with 'image'");

    private final TextField confidenceTextField = new TextField();

    private final TextField vlcLibraryFolderTextField = new TextField();
    private final CheckBox muteVideoCheckBox = new CheckBox("Mute video preview");
    private final CheckBox repeatVideoCheckBox = new CheckBox("Repeat video preview");

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
        alsoImportExtensionsTextField.setTooltip(new Tooltip("Filetypes separated by spaces. E.g. \".txt .pdf .doc\" without quote marks."));
        alsoImportExtensionsTextField.setPromptText("Filetypes separated by spaces. E.g. \".txt .pdf .doc\" without quote marks.");
        h = new HBox(5, new Label("Also import these filetypes:"), alsoImportExtensionsTextField);
        h.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(alsoImportExtensionsTextField, Priority.ALWAYS);
        v.getChildren().add(h);
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
                    if (value < MIN_CONFIDENCE) confidenceTextField.setText("" + MIN_CONFIDENCE);
                    else if (value > MAX_CONFIDENCE) confidenceTextField.setText("" + MAX_CONFIDENCE);
                } catch (NumberFormatException e) {
                    confidenceTextField.setText("" + DEFAULT_CONFIDENCE);
                }
            }
        });
        confidenceTextField.setPromptText(DEFAULT_CONFIDENCE + " recommended");
        h = new HBox(5, new Label("Confidence:"), confidenceTextField, new Label("(" + MIN_CONFIDENCE + "-" + MAX_CONFIDENCE + ")"));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(LEFT20);
        rootV.getChildren().add(h);

        l = new Label("Video viewing");
        l.setFont(BOLD_ITALIC);
        rootV.getChildren().addAll(new Separator(), l);
        vlcLibraryFolderTextField.setPromptText("Leave empty to use default path");
        HBox.setHgrow(vlcLibraryFolderTextField, Priority.ALWAYS);
        browse = new Button("Browse");
        browse.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            if (vlcLibraryFolderTextField.getText() != null && !vlcLibraryFolderTextField.getText().isEmpty())
                dc.setInitialDirectory(new File(vlcLibraryFolderTextField.getText()).getParentFile());
            dc.setTitle("Select folder containing libvlc.dll");
            File result = dc.showDialog(getScene().getWindow());

            if (result != null) {
                vlcLibraryFolderTextField.setText(result.getAbsolutePath());
            }
        });
        h = new HBox(5, new Label("VLC Library Folder:"), vlcLibraryFolderTextField, browse);
        h.setAlignment(Pos.CENTER_LEFT);
        v = new VBox(5, h, muteVideoCheckBox, repeatVideoCheckBox);
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
        settings.setString(Settings.Key.USER_FILETYPES, alsoImportExtensionsTextField.getText());
        settings.setBoolean(Settings.Key.USE_FILENAME_FROM_URL, fileNameFromURLCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_TAGME, tagWithTagmeCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_VIDEO, tagWithVideoCheckBox.isSelected());
        settings.setBoolean(Settings.Key.TAG_IMAGE, tagWithImageCheckBox.isSelected());
        double confidence = DEFAULT_CONFIDENCE;
        try {
            confidence = Double.parseDouble(confidenceTextField.getText());
            if (confidence < MIN_CONFIDENCE) confidence = MIN_CONFIDENCE;
            else if (confidence > MAX_CONFIDENCE) confidence = MAX_CONFIDENCE;
        } catch (NumberFormatException ignored) {
        }
        settings.setDouble(Settings.Key.CONFIDENCE, confidence);
        settings.setString(Settings.Key.VLCJ_PATH, vlcLibraryFolderTextField.getText());
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
        alsoImportExtensionsTextField.setText(settings.getString(Settings.Key.USER_FILETYPES));
        fileNameFromURLCheckBox.setSelected(settings.getBoolean(Settings.Key.USE_FILENAME_FROM_URL));
        tagWithTagmeCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_TAGME));
        tagWithVideoCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_VIDEO));
        tagWithImageCheckBox.setSelected(settings.getBoolean(Settings.Key.TAG_IMAGE));
        double confidence = settings.getDouble(Settings.Key.CONFIDENCE);
        if (confidence < MIN_CONFIDENCE) confidence = MIN_CONFIDENCE;
        else if (confidence > MAX_CONFIDENCE) confidence = MAX_CONFIDENCE;
        confidenceTextField.setText("" + confidence);
        vlcLibraryFolderTextField.setText(settings.getString(Settings.Key.VLCJ_PATH));
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
