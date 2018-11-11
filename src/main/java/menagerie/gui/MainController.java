package menagerie.gui;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import menagerie.gui.grid.ImageGridCell;
import menagerie.gui.grid.ImageGridView;
import menagerie.gui.image.DynamicImageView;
import menagerie.gui.progress.ProgressLockThread;
import menagerie.gui.progress.ProgressLockThreadCancelListener;
import menagerie.gui.progress.ProgressLockThreadFinishListener;
import menagerie.model.SimilarPair;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.search.*;
import menagerie.model.settings.Settings;
import menagerie.util.Filters;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MainController {

    public StackPane rootPane;

    public BorderPane explorerPane;
    public ToggleButton descendingToggleButton;
    public PredictiveTextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultCountLabel;
    public Label imageInfoLabel;
    public Label imageFileNameLabel;
    public ListView<Tag> tagListView;
    public PredictiveTextField editTagsTextField;
    public MenuBar menuBar;
    public Button showErrorsButton;

    public BorderPane settingsPane;
    public CheckBox computeMD5SettingCheckbox;
    public CheckBox computeHistSettingCheckbox;
    public CheckBox buildThumbSettingCheckbox;
    public CheckBox autoImportWebSettingCheckbox;
    public TextField lastFolderSettingTextField;
    public Button settingsCancelButton;
    public ChoiceBox<Integer> gridWidthChoiceBox;
    public TextField dbURLTextField;
    public TextField dbUserTextField;
    public TextField dbPassTextField;
    public CheckBox duplicateComputeMD5SettingCheckbox;
    public CheckBox duplicateComputeHistSettingCheckbox;
    public TextField histConfidenceSettingTextField;
    public CheckBox duplicateConsolidateTagsSettingCheckbox;
    public CheckBox backupDatabaseSettingCheckBox;
    public TextField importFromFolderSettingTextField;
    public CheckBox autoImportFolderSettingCheckBox;
    public Button importFromFolderSettingBrowseButton;
    public CheckBox autoImportFromFolderToDefaultSettingCheckBox;
    public CheckBox duplicateCompareBlackAndWhiteSettingCheckbox;

    public BorderPane tagListPane;
    public ChoiceBox<String> tagListOrderChoiceBox;
    public ListView<Tag> tagListListView;
    public TextField searchTagsScreenTextField;

    public BorderPane helpPane;

    public BorderPane progressLockPane;
    public ProgressBar progressLockProgressBar;
    public Label progressLockTitleLabel;
    public Label progressLockMessageLabel;
    public Label progressLockCountLabel;

    public BorderPane duplicatePane;
    public Label duplicateSimilarityLabel;
    public Label duplicateLeftInfoLabel;
    public TextField duplicateLeftPathTextField;
    public DynamicImageView duplicateLeftImageView;
    public ListView<Tag> duplicateLeftTagListView;
    public Label duplicateRightInfoLabel;
    public TextField duplicateRightPathTextField;
    public DynamicImageView duplicateRightImageView;
    public ListView<Tag> duplicateRightTagListView;

    public BorderPane slideShowPane;
    public DynamicImageView slideShowImageView;

    public BorderPane errorsPane;
    public ListView<TrackedError> errorsListView;

    private FadeTransition screenOpenTransition = new FadeTransition(Duration.millis(100));
    private FadeTransition screenCloseTransition = new FadeTransition(Duration.millis(200));


    private Menagerie menagerie;
    private Search currentSearch = null;

    private ProgressLockThread currentProgressLockThread;
    private ImageInfo currentlyPreviewing = null;
    private String lastTagString = null;
    private List<SimilarPair> currentSimilarPairs = null;
    private SimilarPair currentlyPreviewingPair = null;
    private List<ImageInfo> currentSlideShow = null;
    private ImageInfo currentSlideShowShowing = null;

    private final ClipboardContent clipboard = new ClipboardContent();
    private boolean imageGridViewDragging = false;
    private ContextMenu cellContextMenu;

    private FolderWatcherThread folderWatcherThread = null;

    private final Settings settings = new Settings(new File("menagerie.properties"));


    // ---------------------------------- Initializers ------------------------------------

    @FXML
    public void initialize() {

        //Initialize settings
        try {
            settings.loadFromFile();
        } catch (IOException e) {
            trySaveSettings();
        }

        //Backup database
        try {
            backupDatabase();
        } catch (IOException e) {
            e.printStackTrace();
            Main.showErrorMessage("Error", "Error while trying to back up the database: " + settings.getDbUrl(), e.getLocalizedMessage());
        }

        //Initialize the menagerie
        initMenagerie();

        //Init window listeners
        initWindowListeners();

        //Init screens
        initExplorerScreen();
        initSettingsScreen();
        initTagListScreen();
        initDuplicateScreen();
        initErrorsScreen();

        //Init screen transitions
        screenOpenTransition.setFromValue(0);
        screenOpenTransition.setToValue(1);
        screenCloseTransition.setFromValue(1);
        screenCloseTransition.setToValue(0);

        //Apply a default search
        searchOnAction();

        //Init window props from settings
        Platform.runLater(() -> {
            initWindowPropertiesFromSettings();

            rootPane.getScene().getWindow().setOnCloseRequest(event -> exit());
        });

        //Init folder watcher
        startWatchingFolderForImages();

    }

    private void initErrorsScreen() {
        errorsListView.setCellFactory(param -> new ErrorListCell(error -> errorsListView.getItems().remove(error)));
        errorsListView.getItems().addListener((ListChangeListener<? super TrackedError>) c -> {
            final int count = errorsListView.getItems().size();

            if (count == 0) {
                showErrorsButton.setStyle("-fx-background-color: transparent;");
            } else {
                showErrorsButton.setStyle("-fx-background-color: red;");
            }

            showErrorsButton.setText("" + count);
        });
    }

    private void initMenagerie() {
        try {
            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getDbUrl(), settings.getDbUser(), settings.getDbPass());
            DatabaseVersionUpdater.updateDatabase(db);

            menagerie = new Menagerie(db);

            menagerie.getUpdateQueue().setErrorListener(e -> Platform.runLater(() -> {
                addErrorToList(new TrackedError(e, TrackedError.Severity.HIGH, "Error while updating database", "An exception as thrown while trying to update the database", "Concurrent modification error or SQL statement out of date"));
            }));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void initSettingsScreen() {
        //Initialize grid width setting choicebox
        Integer[] elements = new Integer[Settings.MAX_IMAGE_GRID_WIDTH - Settings.MIN_IMAGE_GRID_WIDTH + 1];
        for (int i = 0; i < elements.length; i++) elements[i] = i + Settings.MIN_IMAGE_GRID_WIDTH;
        gridWidthChoiceBox.getItems().addAll(elements);
        gridWidthChoiceBox.getSelectionModel().clearAndSelect(0);
    }

    private void initWindowPropertiesFromSettings() {
        Stage stage = ((Stage) explorerPane.getScene().getWindow());
        stage.setMaximized(settings.isWindowMaximized());
        if (settings.getWindowWidth() > 0) stage.setWidth(settings.getWindowWidth());
        if (settings.getWindowHeight() > 0) stage.setHeight(settings.getWindowHeight());
        if (settings.getWindowX() >= 0) stage.setX(settings.getWindowX());
        if (settings.getWindowY() >= 0) stage.setY(settings.getWindowY());
    }

    private void initDuplicateScreen() {
        duplicateLeftTagListView.setCellFactory(param -> new TagListCell());
        duplicateRightTagListView.setCellFactory(param -> new TagListCell());
        histConfidenceSettingTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double d = Double.parseDouble(histConfidenceSettingTextField.getText());
                    if (d <= 0 || d > 1) {
                        histConfidenceSettingTextField.setText("0.95");
                    }
                } catch (NullPointerException | NumberFormatException e) {
                    histConfidenceSettingTextField.setText("0.95");
                }
            }
        });
    }

    private void initTagListScreen() {
        //Initialize tagList order choicebox
        tagListOrderChoiceBox.getItems().addAll("Name", "ID", "Frequency");
        tagListOrderChoiceBox.getSelectionModel().clearAndSelect(0);
        tagListOrderChoiceBox.setOnAction(event -> updateTagListListViewOrder());

        tagListListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                MenuItem i1 = new MenuItem("Search this tag");
                i1.setOnAction(event1 -> {
                    searchTextField.setText(c.getItem().getName());
                    searchTextField.positionCaret(searchTextField.getText().length());
                    closeTagListScreen();
                    searchOnAction();
                });
                ContextMenu m = new ContextMenu(i1);
                m.show(c, event.getScreenX(), event.getScreenY());
            });
            return c;
        });

        searchTagsScreenTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            tagListListView.getItems().clear();
            menagerie.getTags().forEach(tag -> {
                if (tag.getName().toLowerCase().startsWith(newValue.toLowerCase())) tagListListView.getItems().add(tag);
            });
            updateTagListListViewOrder();
        });
    }

    private void initExplorerScreen() {
        //Set image grid width from settings
        setImageGridWidth(settings.getImageGridWidth());

        //Init image grid
        imageGridView.setSelectionListener(this::previewImage);
        imageGridView.setCellFactory(param -> {
            ImageGridCell c = new ImageGridCell();
            c.setOnDragDetected(event -> {
                if (!imageGridView.getSelected().isEmpty() && event.isPrimaryButtonDown()) {
                    if (!imageGridView.isSelected(c.getItem()))
                        imageGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());

                    Dragboard db = c.startDragAndDrop(TransferMode.ANY);

                    for (ImageInfo img : imageGridView.getSelected()) {
                        String filename = img.getFile().getName().toLowerCase();
                        if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")) {
                            db.setDragView(img.getThumbnail());
                            break;
                        }
                    }

                    List<File> files = new ArrayList<>();
                    imageGridView.getSelected().forEach(img -> files.add(img.getFile()));
                    clipboard.putFiles(files);
                    db.setContent(clipboard);

                    imageGridViewDragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                imageGridViewDragging = false;
                event.consume();
            });
            c.setOnMouseReleased(event -> {
                if (!imageGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
                    imageGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                if (cellContextMenu.isShowing()) cellContextMenu.hide();
                cellContextMenu.show(c, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            return c;
        });
        imageGridView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE:
                    imageGridCellDeleteEvent(!event.isControlDown());
                    event.consume();
                    break;
            }
        });
        initImageGridViewCellContextMenu();

        //Init drag/drop handlers
        explorerPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        explorerPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                List<Runnable> queue = new ArrayList<>();
                files.forEach(file -> queue.add(() -> {
                    try {
                        menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                    } catch (Exception e) {
                        Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import image", "Exception was thrown while trying to import an image: " + file, "Unknown")));
                    }
                }));

                if (!queue.isEmpty()) {
                    openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
                }
            } else if (url != null && !url.isEmpty()) {
                Platform.runLater(() -> {
                    String folder = settings.getDefaultFolder();
                    if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                    String filename = URI.create(url).getPath().replaceAll("^.*/", "");
                    File target = resolveDuplicateFilename(new File(folder + filename));

                    while (!settings.isAutoImportFromWeb() || !target.getParentFile().exists() || target.exists() || !Filters.IMAGE_FILTER.accept(target)) {
                        target = openSaveImageDialog(new File(settings.getDefaultFolder()), filename);
                        if (target == null) return;
                        if (target.exists())
                            Main.showErrorMessage("Error", "File already exists, cannot be overwritten", target.getAbsolutePath());
                    }

                    final File finalTarget = target;
                    new Thread(() -> {
                        try {
                            downloadAndSaveFile(url, finalTarget);
                            Platform.runLater(() -> {
                                ImageInfo img = menagerie.importImage(finalTarget, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                                if (img == null) {
                                    if (!finalTarget.delete())
                                        System.out.println("Tried to delete a downloaded file, as it couldn't be imported, but failed: " + finalTarget);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> Main.showErrorMessage("Unexpected error", "Error while trying to download image", e.getLocalizedMessage()));
                        }
                    }).start();
                });
            }
            event.consume();
        });

        //Init tag list cell factory
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    MenuItem i1 = new MenuItem("Add to search");
                    i1.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " " + c.getItem().getName());
                        searchOnAction();
                    });
                    MenuItem i2 = new MenuItem("Exclude from search");
                    i2.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        searchOnAction();
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> imageGridView.getSelected().forEach(img -> img.removeTag(c.getItem())));
                    ContextMenu m = new ContextMenu(i1, i2, new SeparatorMenuItem(), i3);
                    m.show(c, event.getScreenX(), event.getScreenY());
                }
            });
            return c;
        });

        editTagsTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags;
            if (negative) tags = new ArrayList<>(tagListView.getItems());
            else tags = new ArrayList<>(menagerie.getTags());
            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) results.add("-" + tag.getName());
                    else results.add(tag.getName());
                }

                if (results.size() >= 8) break;
            }

            return results;
        });

        searchTextField.setTop(false);
        searchTextField.setOptionsListener(editTagsTextField.getOptionsListener());
    }

    private void initImageGridViewCellContextMenu() {
        MenuItem si1 = new MenuItem("Selected");
        si1.setOnAction(event1 -> openSlideShowScreen(imageGridView.getSelected()));
        MenuItem si2 = new MenuItem("Searched");
        si2.setOnAction(event1 -> openSlideShowScreen(imageGridView.getItems()));
        Menu i1 = new Menu("Slideshow", null, si1, si2);

        MenuItem i2 = new MenuItem("Open in Explorer");
        i2.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + imageGridView.getLastSelected().getFile().getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Unexpected Error", "Error opening file explorer", e.getLocalizedMessage());
                }
            }
        });

        MenuItem i3 = new MenuItem("Build MD5 Hash");
        i3.setOnAction(event1 -> {
            List<Runnable> queue = new ArrayList<>();
            imageGridView.getSelected().forEach(img -> {
                if (img.getMD5() == null) {
                    queue.add(() -> {
                        try {
                            img.initializeMD5();
                            img.commitMD5ToDatabase();
                        } catch (Exception e) {
                            Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute MD5", "Exception was thrown while trying to compute an MD5 for image: " + img, "Unknown")));
                        }
                    });
                }
            });
            if (!queue.isEmpty()) {
                openProgressLockScreen("Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue, null, null);
            }
        });
        MenuItem i4 = new MenuItem("Build Histogram");
        i4.setOnAction(event1 -> {
            List<Runnable> queue = new ArrayList<>();
            imageGridView.getSelected().forEach(img -> {
                String filename = img.getFile().getName().toLowerCase();
                if (img.getHistogram() == null && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
                    queue.add(() -> {
                        try {
                            img.initializeHistogram();
                            img.commitHistogramToDatabase();
                        } catch (Exception e) {
                            Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute histogram", "Exception was thrown while trying to compute a histogram for image: " + img, "Unknown")));
                        }
                    });
                }
            });
            if (!queue.isEmpty()) {
                openProgressLockScreen("Building Histograms", "Building image histograms for " + queue.size() + " files...", queue, null, null);
            }
        });

        MenuItem i5 = new MenuItem("Find Duplicates");
        i5.setOnAction(event1 -> processAndShowDuplicates(imageGridView.getSelected()));

        MenuItem i6 = new MenuItem("Move To...");
        i6.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Move files to folder...");
                File result = dc.showDialog(rootPane.getScene().getWindow());

                if (result != null) {
                    List<Runnable> queue = new ArrayList<>();

                    imageGridView.getSelected().forEach(img -> queue.add(() -> {
                        File f = result.toPath().resolve(img.getFile().getName()).toFile();
                        if (!img.getFile().equals(f)) {
                            File dest = MainController.resolveDuplicateFilename(f);

                            if (!img.renameTo(dest)) {
                                Platform.runLater(() -> {
                                    addErrorToList(new TrackedError(null, TrackedError.Severity.HIGH, "Error moving image", "An exception was thrown while trying to move an image\nFrom: " + img.getFile() + "\nTo: " + dest, "Unknown"));
                                });
                            }
                        }
                    }));

                    if (!queue.isEmpty()) {
                        openProgressLockScreen("Moving files", "Moving " + queue.size() + " files...", queue, null, null);
                    }
                }
            }
        });

        MenuItem i7 = new MenuItem("Remove");
        i7.setOnAction(event1 -> imageGridCellDeleteEvent(false));
        MenuItem i8 = new MenuItem("Delete");
        i8.setOnAction(event1 -> imageGridCellDeleteEvent(true));

        cellContextMenu = new ContextMenu(i1, new SeparatorMenuItem(), i2, new SeparatorMenuItem(), i3, i4, new SeparatorMenuItem(), i5, new SeparatorMenuItem(), i6, new SeparatorMenuItem(), i7, i8);
    }

    private void initWindowListeners() {
        Platform.runLater(() -> {
            Stage stage = ((Stage) rootPane.getScene().getWindow());

            //Bind window properties to settings
            stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.setWindowMaximized(newValue));
            stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.setWindowWidth(newValue.intValue()));
            stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.setWindowHeight(newValue.intValue()));
            stage.xProperty().addListener((observable, oldValue, newValue) -> settings.setWindowX(newValue.intValue()));
            stage.yProperty().addListener((observable, oldValue, newValue) -> settings.setWindowY(newValue.intValue()));
        });
    }

    // ---------------------------------- Screen/Dialog openers ------------------------------------

    private File openSaveImageDialog(File folder, String filename) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save file from web");
        if (filename != null) fc.setInitialFileName(filename);
        if (folder != null) fc.setInitialDirectory(folder);
        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
        return fc.showSaveDialog(explorerPane.getScene().getWindow());
    }

    private void openSettingsScreen() {
        //Update settings fx nodes
        lastFolderSettingTextField.setText(settings.getDefaultFolder());
        importFromFolderSettingTextField.setText(settings.getImportFromFolderPath());
        dbURLTextField.setText(settings.getDbUrl());
        dbUserTextField.setText(settings.getDbUser());
        dbPassTextField.setText(settings.getDbPass());

        autoImportWebSettingCheckbox.setSelected(settings.isAutoImportFromWeb());
        computeMD5SettingCheckbox.setSelected(settings.isComputeMD5OnImport());
        computeHistSettingCheckbox.setSelected(settings.isComputeHistogramOnImport());
        buildThumbSettingCheckbox.setSelected(settings.isBuildThumbnailOnImport());
        duplicateComputeMD5SettingCheckbox.setSelected(settings.isComputeMD5ForSimilarity());
        duplicateComputeHistSettingCheckbox.setSelected(settings.isComputeHistogramForSimilarity());
        duplicateConsolidateTagsSettingCheckbox.setSelected(settings.isConsolidateTags());
        backupDatabaseSettingCheckBox.setSelected(settings.isBackupDatabase());
        autoImportFolderSettingCheckBox.setSelected(settings.isAutoImportFromFolder());
        autoImportFromFolderToDefaultSettingCheckBox.setSelected(settings.isAutoImportFromFolderToDefault());
        duplicateCompareBlackAndWhiteSettingCheckbox.setSelected(settings.isCompareBlackAndWhiteHists());

        histConfidenceSettingTextField.setText("" + settings.getSimilarityThreshold());

        gridWidthChoiceBox.getSelectionModel().select((Integer) settings.getImageGridWidth());

        updateAutoImportFolderDisabledStatus();

        //Enable pane
        explorerPane.setDisable(true);
        settingsPane.setDisable(false);
        settingsCancelButton.requestFocus();
        startScreenTransition(screenOpenTransition, settingsPane);
    }

    private void closeSettingsScreen(boolean saveChanges) {
        //Disable pane
        explorerPane.setDisable(false);
        settingsPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, settingsPane);

        if (saveChanges) {
            //Save settings to settings object
            settings.setDefaultFolder(lastFolderSettingTextField.getText());
            settings.setDbUrl(dbURLTextField.getText());
            settings.setDbUser(dbUserTextField.getText());
            settings.setDbPass(dbPassTextField.getText());
            settings.setImportFromFolderPath(importFromFolderSettingTextField.getText());

            settings.setAutoImportFromWeb(autoImportWebSettingCheckbox.isSelected());
            settings.setComputeMD5OnImport(computeMD5SettingCheckbox.isSelected());
            settings.setComputeHistogramOnImport(computeHistSettingCheckbox.isSelected());
            settings.setBuildThumbnailOnImport(buildThumbSettingCheckbox.isSelected());
            settings.setComputeMD5ForSimilarity(duplicateComputeMD5SettingCheckbox.isSelected());
            settings.setComputeHistogramForSimilarity(duplicateComputeHistSettingCheckbox.isSelected());
            settings.setConsolidateTags(duplicateConsolidateTagsSettingCheckbox.isSelected());
            settings.setBackupDatabase(backupDatabaseSettingCheckBox.isSelected());
            settings.setAutoImportFromFolder(autoImportFolderSettingCheckBox.isSelected());
            settings.setAutoImportFromFolderToDefault(autoImportFromFolderToDefaultSettingCheckBox.isSelected());
            settings.setCompareBlackAndWhiteHists(duplicateCompareBlackAndWhiteSettingCheckbox.isSelected());

            settings.setSimilarityThreshold(Double.parseDouble(histConfidenceSettingTextField.getText()));

            settings.setImageGridWidth(gridWidthChoiceBox.getValue());

            setImageGridWidth(gridWidthChoiceBox.getValue());

            startWatchingFolderForImages();
        }

        trySaveSettings();
    }

    private void openTagListScreen() {
        tagListListView.getItems().clear();
        tagListListView.getItems().addAll(menagerie.getTags());
        updateTagListListViewOrder();

        explorerPane.setDisable(true);
        tagListPane.setDisable(false);
        tagListPane.requestFocus();
        startScreenTransition(screenOpenTransition, tagListPane);
    }

    private void closeTagListScreen() {
        explorerPane.setDisable(false);
        tagListPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, tagListPane);
    }

    private void openHelpScreen() {
        explorerPane.setDisable(true);
        helpPane.setDisable(false);
        helpPane.requestFocus();
        startScreenTransition(screenOpenTransition, helpPane);
    }

    private void closeHelpScreen() {
        explorerPane.setDisable(false);
        helpPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, helpPane);
    }

    @SuppressWarnings("SameParameterValue")
    private void openProgressLockScreen(String title, String message, List<Runnable> queue, ProgressLockThreadFinishListener finishListener, ProgressLockThreadCancelListener cancelListener) {
        if (currentProgressLockThread != null) currentProgressLockThread.stopRunning();

        currentProgressLockThread = new ProgressLockThread(queue);
        currentProgressLockThread.setUpdateListener((num, total) -> Platform.runLater(() -> {
            final double progress = (double) num / total;
            progressLockProgressBar.setProgress(progress);
            progressLockCountLabel.setText((int) (progress * 100) + "% - " + (total - num) + " remaining...");
        }));
        currentProgressLockThread.setCancelListener((num, total) -> {
            Platform.runLater(this::closeProgressLockScreen);
            if (cancelListener != null) cancelListener.progressCanceled(num, total);
        });
        currentProgressLockThread.setFinishListener(total -> {
            Platform.runLater(this::closeProgressLockScreen);
            if (finishListener != null) finishListener.progressFinished(total);
        });
        currentProgressLockThread.start();

        progressLockTitleLabel.setText(title);
        progressLockMessageLabel.setText(message);
        progressLockProgressBar.setProgress(0);
        progressLockCountLabel.setText("0/" + queue.size());

        explorerPane.setDisable(true);
        progressLockPane.setDisable(false);
        progressLockPane.requestFocus();
        startScreenTransition(screenOpenTransition, progressLockPane);
    }

    private void closeProgressLockScreen() {
        if (currentProgressLockThread != null) currentProgressLockThread.stopRunning();

        explorerPane.setDisable(false);
        progressLockPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, progressLockPane);
    }

    private void openDuplicateScreen(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) return;

        currentSimilarPairs = new ArrayList<>();
        List<Runnable> queue = new ArrayList<>();

        for (int actualI = 0; actualI < images.size(); actualI++) {
            final int i = actualI;
            queue.add(() -> {
                ImageInfo i1 = images.get(i);
                for (int j = i + 1; j < images.size(); j++) {
                    ImageInfo i2 = images.get(j);

                    try {
                        double similarity = i1.getSimilarityTo(i2, settings.isCompareBlackAndWhiteHists());

                        if (similarity >= settings.getSimilarityThreshold()) currentSimilarPairs.add(new SimilarPair(i1, i2, similarity));
                    } catch (Exception e) {
                        Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compare images", "Exception was thrown while trying to compare two images: (" + i1 + ", " + i2 + ")", "Unknown")));
                    }
                }
            });
        }

        if (queue.size() > 5000) {
            openProgressLockScreen("Comparing images", "Checking comparisons for " + queue.size() + " images...", queue, total -> Platform.runLater(() -> {
                if (currentSimilarPairs.isEmpty()) return;

                previewSimilarPair(currentSimilarPairs.get(0));

                explorerPane.setDisable(true);
                duplicatePane.setDisable(false);
                duplicatePane.setOpacity(1);
                duplicatePane.requestFocus();
            }), null);
        } else {
            queue.forEach(Runnable::run);

            if (currentSimilarPairs.isEmpty()) return;

            previewSimilarPair(currentSimilarPairs.get(0));

            explorerPane.setDisable(true);
            duplicatePane.setDisable(false);
            duplicatePane.requestFocus();
            startScreenTransition(screenOpenTransition, duplicatePane);
        }
    }

    private void closeDuplicateScreen() {
        previewSimilarPair(null);

        explorerPane.setDisable(false);
        duplicatePane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, duplicatePane);
    }

    private void openSlideShowScreen(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) return;

        currentSlideShow = images;
        currentSlideShowShowing = images.get(0);
        slideShowImageView.setImage(currentSlideShowShowing.getImage());

        explorerPane.setDisable(true);
        slideShowPane.setDisable(false);
        slideShowPane.requestFocus();
        startScreenTransition(screenOpenTransition, slideShowPane);
    }

    private void closeSlideShowScreen() {
        explorerPane.setDisable(false);
        slideShowPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, slideShowPane);
    }

    private void openErrorsScreen() {
        explorerPane.setDisable(true);
        errorsPane.setDisable(false);
        errorsPane.requestFocus();
        startScreenTransition(screenOpenTransition, errorsPane);
    }

    private void closeErrorsScreen() {
        explorerPane.setDisable(false);
        errorsPane.setDisable(true);
        imageGridView.requestFocus();
        startScreenTransition(screenCloseTransition, errorsPane);
    }

    private void requestImportFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        if (settings.getDefaultFolder() != null && !settings.getDefaultFolder().isEmpty())
            dc.setInitialDirectory(new File(settings.getDefaultFolder()));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            List<Runnable> queue = new ArrayList<>();
            List<File> files = getFilesRecursive(result, Filters.IMAGE_FILTER);
            menagerie.getImages().forEach(img -> files.remove(img.getFile()));
            files.forEach(file -> queue.add(() -> {
                try {
                    menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                } catch (Exception e) {
                    Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import image", "Exception was thrown while trying to import an image: " + file, "Unknown")));
                }
            }));

            if (!queue.isEmpty()) {
                openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    private void requestImportFiles() {
        FileChooser fc = new FileChooser();
        if (settings.getDefaultFolder() != null && !settings.getDefaultFolder().isEmpty())
            fc.setInitialDirectory(new File(settings.getDefaultFolder()));
        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
        List<File> results = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (results != null && !results.isEmpty()) {
            final List<File> finalResults = new ArrayList<>(results);
            menagerie.getImages().forEach(img -> finalResults.remove(img.getFile()));

            List<Runnable> queue = new ArrayList<>();
            finalResults.forEach(file -> queue.add(() -> {
                try {
                    menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                } catch (Exception e) {
                    Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to import image", "Exception was thrown while trying to import an image: " + file, "Unknown")));
                }
            }));

            if (!queue.isEmpty()) {
                openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    // ---------------------------------- GUI Action Methods ------------------------------------

    @SuppressWarnings("SameParameterValue")
    private void previewImage(ImageInfo image) {
        if (currentlyPreviewing != null) currentlyPreviewing.setTagListener(null);
        currentlyPreviewing = image;

        if (image != null) {
            image.setTagListener(() -> updateTagListViewContents(image));

            previewImageView.setImage(image.getImage());

            updateTagListViewContents(image);

            imageFileNameLabel.setText(image.getFile().toString());
            updateImageInfoLabel(image, imageInfoLabel);
        } else {
            previewImageView.setImage(null);
            tagListView.getItems().clear();

            imageFileNameLabel.setText("N/A");
            updateImageInfoLabel(null, imageInfoLabel);
        }
    }

    private void updateTagListViewContents(ImageInfo image) {
        tagListView.getItems().clear();
        tagListView.getItems().addAll(image.getTags());
        tagListView.getItems().sort(Comparator.comparing(Tag::getName));
    }

    private static void updateImageInfoLabel(ImageInfo image, Label label) {
        if (image == null) {
            label.setText("Size: N/A - Res: N/A");

            return;
        }

        if (image.getImage().isBackgroundLoading() && image.getImage().getProgress() != 1) {
            label.setText("Size: N/A - Res: N/A");

            image.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1 && !image.getImage().isError()) updateImageInfoLabel(image, label);
            });
        } else {
            //Find size string
            double size = image.getFile().length();
            String sizeStr;
            if (size > 1024 * 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024 / 1024) + "GB";
            else if (size > 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024) + "MB";
            else if (size > 1024) sizeStr = String.format("%.2f", size / 1024) + "KB";
            else sizeStr = String.format("%.2f", size) + "B";

            label.setText("Size: " + sizeStr + " - Res: " + (int) image.getImage().getWidth() + "x" + (int) image.getImage().getHeight());
        }
    }

    private void updateAutoImportFolderDisabledStatus() {
        if (autoImportFolderSettingCheckBox.isSelected()) {
            importFromFolderSettingTextField.setDisable(false);
            importFromFolderSettingBrowseButton.setDisable(false);
            autoImportFromFolderToDefaultSettingCheckBox.setDisable(false);
        } else {
            importFromFolderSettingTextField.setDisable(true);
            importFromFolderSettingBrowseButton.setDisable(true);
            autoImportFromFolderToDefaultSettingCheckBox.setDisable(true);
        }
    }

    private void searchOnAction() {
        previewImageView.setImage(null);

        final boolean descending = descendingToggleButton.isSelected();

        List<SearchRule> rules = constructRuleSet(searchTextField.getText());

        if (currentSearch != null) currentSearch.close();
        currentSearch = new Search(menagerie, rules, descending);
        currentSearch.setListener(new SearchUpdateListener() {
            @Override
            public void imageAdded(ImageInfo img) {
                currentSearch.sortResults();
                Platform.runLater(() -> imageGridView.getItems().add(currentSearch.getResults().indexOf(img), img));
            }

            @Override
            public void imageRemoved(ImageInfo img) {
                Platform.runLater(() -> {
                    int index = imageGridView.getItems().indexOf(img) + 1;
                    if (index < imageGridView.getItems().size())
                        imageGridView.setLastSelected(imageGridView.getItems().get(index));
                    else if (index - 1 >= 0) imageGridView.setLastSelected(imageGridView.getItems().get(index - 1));

                    if (img.equals(currentlyPreviewing)) previewImage(null);

                    imageGridView.deselect(img);
                    imageGridView.getItems().remove(img);
                });
            }
        });

        resultCountLabel.setText("Results: " + currentSearch.getResults().size());
        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(currentSearch.getResults());

        if (!imageGridView.getItems().isEmpty()) imageGridView.select(imageGridView.getItems().get(0), false, false);
    }

    private List<SearchRule> constructRuleSet(String str) {
        List<SearchRule> rules = new ArrayList<>();
        for (String arg : str.split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            if (arg.startsWith("id:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                IDRule.Type type = IDRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = IDRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = IDRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new IDRule(type, Integer.parseInt(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for ID rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = DateAddedRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = DateAddedRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new DateAddedRule(type, Long.parseLong(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting long value for date added rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("md5:")) {
                rules.add(new MD5Rule(arg.substring(arg.indexOf(':') + 1)));
            } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
                rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1)));
            } else if (arg.startsWith("missing:")) {
                String type = arg.substring(arg.indexOf(':') + 1);
                switch (type.toLowerCase()) {
                    case "md5":
                        rules.add(new MissingRule(MissingRule.Type.MD5));
                        break;
                    case "file":
                        rules.add(new MissingRule(MissingRule.Type.FILE));
                        break;
                    case "histogram":
                    case "hist":
                        rules.add(new MissingRule(MissingRule.Type.HISTOGRAM));
                        break;
                }
            } else if (arg.startsWith("tags:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                TagCountRule.Type type = TagCountRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = TagCountRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = TagCountRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new TagCountRule(type, Integer.parseInt(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for tag count rule", e.getLocalizedMessage());
                }
            } else {
                boolean exclude = false;
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    exclude = true;
                }

                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, exclude));
            }
        }
        return rules;
    }

    private void setImageGridWidth(int n) {
        final double width = 18 + (ImageInfo.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * n;
        imageGridView.setMinWidth(width);
        imageGridView.setMaxWidth(width);
        imageGridView.setPrefWidth(width);
    }

    private void previewSimilarPair(SimilarPair pair) {
        if (pair == null) {
            currentlyPreviewingPair = null;

            duplicateLeftImageView.setImage(null);
            duplicateLeftTagListView.getItems().clear();
            duplicateLeftPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicateLeftInfoLabel);

            duplicateRightImageView.setImage(null);
            duplicateRightTagListView.getItems().clear();
            duplicateRightPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicateRightInfoLabel);

            duplicateSimilarityLabel.setText("N/A% Match");
        } else {
            currentlyPreviewingPair = pair;

            duplicateLeftImageView.setImage(pair.getImg1().getImage());
            duplicateLeftPathTextField.setText(pair.getImg1().getFile().toString());
            duplicateLeftTagListView.getItems().clear();
            duplicateLeftTagListView.getItems().addAll(pair.getImg1().getTags());
            duplicateLeftTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg1(), duplicateLeftInfoLabel);

            duplicateRightImageView.setImage(pair.getImg2().getImage());
            duplicateRightPathTextField.setText(pair.getImg2().getFile().toString());
            duplicateRightTagListView.getItems().clear();
            duplicateRightTagListView.getItems().addAll(pair.getImg2().getTags());
            duplicateRightTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg2(), duplicateRightInfoLabel);

            DecimalFormat df = new DecimalFormat("#.##");
            duplicateSimilarityLabel.setText((currentSimilarPairs.indexOf(pair) + 1) + "/" + currentSimilarPairs.size() + " - " + df.format(pair.getSimilarity() * 100) + "% Match");
        }
    }

    private void previewLastSimilarPair() {
        if (currentSimilarPairs == null || currentSimilarPairs.isEmpty()) return;

        if (currentlyPreviewingPair == null) {
            previewSimilarPair(currentSimilarPairs.get(0));
        } else {
            int i = currentSimilarPairs.indexOf(currentlyPreviewingPair);
            if (i > 0) {
                previewSimilarPair(currentSimilarPairs.get(i - 1));
            } else {
                previewSimilarPair(currentSimilarPairs.get(0));
            }
        }
    }

    private void previewNextSimilarPair() {
        if (currentSimilarPairs == null || currentSimilarPairs.isEmpty()) return;

        if (currentlyPreviewingPair == null) {
            previewSimilarPair(currentSimilarPairs.get(0));
        } else {
            int i = currentSimilarPairs.indexOf(currentlyPreviewingPair);
            if (i >= 0) {
                if (i + 1 < currentSimilarPairs.size()) previewSimilarPair(currentSimilarPairs.get(i + 1));
            } else {
                previewSimilarPair(currentSimilarPairs.get(0));
            }
        }
    }

    private void updateTagListListViewOrder() {
        switch (tagListOrderChoiceBox.getValue()) {
            case "ID":
                tagListListView.getItems().sort(Comparator.comparingInt(Tag::getId));
                break;
            case "Frequency":
                tagListListView.getItems().sort(Comparator.comparingInt(Tag::getFrequency).reversed());
                break;
            case "Name":
                tagListListView.getItems().sort(Comparator.comparing(Tag::getName));
                break;
        }
    }

    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || imageGridView.getSelected().isEmpty()) return;
        lastTagString = input.trim();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    new ArrayList<>(imageGridView.getSelected()).forEach(img -> img.removeTag(t)); // New arraylist to avoid concurrent modification
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (ImageInfo img : new ArrayList<>(imageGridView.getSelected())) { // New arraylist to avoid concurrent modification
                    img.addTag(t);
                }
            }
        }
    }

    private void deleteDuplicateImageEvent(ImageInfo toDelete, ImageInfo toKeep) {
        int index = currentSimilarPairs.indexOf(currentlyPreviewingPair);
        toDelete.remove(true);

        //Consolidate tags
        if (settings.isConsolidateTags()) {
            toDelete.getTags().forEach(toKeep::addTag);
        }

        //Remove other pairs containing the deleted image
        for (SimilarPair pair : new ArrayList<>(currentSimilarPairs)) {
            if (toDelete.equals(pair.getImg1()) || toDelete.equals(pair.getImg2())) {
                int i = currentSimilarPairs.indexOf(pair);
                currentSimilarPairs.remove(pair);
                if (i < index) {
                    index--;
                }
            }
        }

        if (index > currentSimilarPairs.size() - 1) index = currentSimilarPairs.size() - 1;

        if (currentSimilarPairs.isEmpty()) {
            closeDuplicateScreen();
        } else {
            previewSimilarPair(currentSimilarPairs.get(index));
        }
    }

    private void processAndShowDuplicates(List<ImageInfo> images) {
        if (settings.isComputeMD5ForSimilarity()) {
            List<Runnable> queue = new ArrayList<>();

            images.forEach(i -> {
                if (i.getMD5() == null) queue.add(() -> {
                    try {
                        i.initializeMD5();
                        i.commitMD5ToDatabase();
                    } catch (Exception e) {
                        Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute MD5", "Exception was thrown while trying to compute MD5 for image: " + i, "Unknown")));
                    }
                });
            });

            openProgressLockScreen("Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue, total -> {
                //TODO: Fix this. If md5 computing is disabled, histogram building won't happen
                if (settings.isComputeHistogramForSimilarity()) {
                    List<Runnable> queue2 = new ArrayList<>();

                    images.forEach(i -> {
                        String filename = i.getFile().getName().toLowerCase();
                        if (i.getHistogram() == null && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")))
                            queue2.add(() -> {
                                try {
                                    i.initializeHistogram();
                                    i.commitHistogramToDatabase();
                                } catch (Exception e) {
                                    Platform.runLater(() -> addErrorToList(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute histogram", "Exception was thrown while trying to compute a histogram for image: " + i, "Unknown")));
                                }
                            });
                    });

                    Platform.runLater(() -> openProgressLockScreen("Building Histograms", "Building histograms for " + queue2.size() + " files...", queue2, total1 -> Platform.runLater(() -> openDuplicateScreen(images)), null));
                } else {
                    Platform.runLater(() -> openDuplicateScreen(images));
                }
            }, null);
        } else {
            openDuplicateScreen(images);
        }
    }

    private void slideShowShowNext() {
        int i = currentSlideShow.indexOf(currentSlideShowShowing);
        if (i + 1 < currentSlideShow.size()) currentSlideShowShowing = currentSlideShow.get(i + 1);
        slideShowImageView.setImage(currentSlideShowShowing.getImage());
    }

    private void slideShowShowPrevious() {
        int i = currentSlideShow.indexOf(currentSlideShowShowing);
        if (i - 1 >= 0) currentSlideShowShowing = currentSlideShow.get(i - 1);
        slideShowImageView.setImage(currentSlideShowShowing.getImage());
    }

    private void startWatchingFolderForImages() {
        if (folderWatcherThread != null) {
            folderWatcherThread.stopWatching();
        }

        if (settings.isAutoImportFromFolder()) {
            File watchFolder = new File(settings.getImportFromFolderPath());
            if (watchFolder.exists() && watchFolder.isDirectory()) {
                folderWatcherThread = new FolderWatcherThread(watchFolder, Filters.IMAGE_FILTER, 30000, files -> {
                    for (File file : files) {
                        if (settings.isAutoImportFromFolderToDefault()) {
                            String folder = settings.getDefaultFolder();
                            if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                            File f = new File(folder + file.getName());
                            if (file.equals(f)) continue; //File is being "moved" to same folder

                            File dest = resolveDuplicateFilename(f);

                            if (!file.renameTo(dest)) {
                                continue;
                            }

                            file = dest;
                        }

                        if (menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport()) == null) {
                            if (!file.delete())
                                System.out.println("Failed to delete file after it was denied by the Menagerie");
                        }
                    }
                });
                folderWatcherThread.setDaemon(true);
                folderWatcherThread.start();
            }
        }
    }

    private void exit() {
        trySaveSettings();

        new Thread(() -> {
            try {
                System.out.println("Attempting to shut down Menagerie database and defragment the file");
                menagerie.getDatabase().createStatement().executeUpdate("SHUTDOWN DEFRAG;");
                System.out.println("Done defragging database file");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();

        Platform.exit();
    }

    private void imageGridCellDeleteEvent(boolean deleteFiles) {
        if (!imageGridView.getSelected().isEmpty()) {
            Alert d = new Alert(Alert.AlertType.CONFIRMATION);

            if (deleteFiles) {
                d.setTitle("Delete files");
                d.setHeaderText("Permanently delete selected files? (" + imageGridView.getSelected().size() + " files)");
                d.setContentText("This action CANNOT be undone (files will be deleted)");
            } else {
                d.setTitle("Forget files");
                d.setHeaderText("Remove selected files from database? (" + imageGridView.getSelected().size() + " files)");
                d.setContentText("This action CANNOT be undone");
            }

            Optional result = d.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new ArrayList<>(imageGridView.getSelected()).forEach(img -> img.remove(deleteFiles));
            }
        }
    }

    private void startScreenTransition(FadeTransition ft, Node screen) {
        boolean openRunning = screenOpenTransition.getStatus().equals(Animation.Status.RUNNING);
        boolean closeRunning = screenCloseTransition.getStatus().equals(Animation.Status.RUNNING);

        screenOpenTransition.stop();
        screenCloseTransition.stop();

        if (openRunning) screenOpenTransition.getNode().setOpacity(1);
        if (closeRunning) screenCloseTransition.getNode().setOpacity(0);

        ft.setNode(screen);
        ft.playFromStart();
    }

    private void addErrorToList(TrackedError error) {
        errorsListView.getItems().add(0, error);
        Toolkit.getDefaultToolkit().beep();
    }

    // ---------------------------------- Compute Utilities ------------------------------------

    private static void downloadAndSaveFile(String url, File target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/4.0");
        ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
        FileOutputStream fos = new FileOutputStream(target);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        conn.disconnect();
        rbc.close();
        fos.close();
    }

    private static List<File> getFilesRecursive(File folder, FileFilter filter) {
        File[] files = folder.listFiles();
        List<File> results = new ArrayList<>();
        if (files == null) return results;

        for (File file : files) {
            if (file.isDirectory()) {
                results.addAll(getFilesRecursive(file, filter));
            } else {
                if (filter.accept(file)) results.add(file);
            }
        }
        return results;
    }

    private void backupDatabase() throws IOException {
        if (!settings.isBackupDatabase()) return;

        String path = settings.getDbUrl() + ".mv.db";
        if (path.startsWith("~")) {
            String temp = System.getProperty("user.home");
            if (!temp.endsWith("/") && !temp.endsWith("\\")) temp += "/";
            path = path.substring(1);
            if (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);

            path = temp + path;
        }

        File currentDatabaseFile = new File(path);

        if (currentDatabaseFile.exists()) {
            System.out.println("Backing up database at: " + currentDatabaseFile);
            File backupFile = new File(path + ".bak");
            Files.copy(currentDatabaseFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully backed up database to: " + backupFile);
        }
    }

    private static File resolveDuplicateFilename(File file) {
        while (file.exists()) {
            String name = file.getName();
            if (name.matches(".*\\s\\([0-9]+\\)\\..*")) {
                int count = Integer.parseInt(name.substring(name.lastIndexOf('(') + 1, name.lastIndexOf(')')));
                name = name.substring(0, name.lastIndexOf('(') + 1) + (count + 1) + name.substring(name.lastIndexOf(')'));
            } else {
                name = name.substring(0, name.lastIndexOf('.')) + " (2)" + name.substring(name.lastIndexOf('.'));
            }

            String parent = file.getParent();
            if (!parent.endsWith("/") && !parent.endsWith("\\")) parent += "/";
            file = new File(parent + name);
        }

        return file;
    }

    private void trySaveSettings() {
        try {
            settings.saveToFile();
        } catch (IOException e1) {
            Platform.runLater(() -> addErrorToList(new TrackedError(e1, TrackedError.Severity.HIGH, "Unable to save properties", "IO Exception thrown while trying to save properties file", "1.) Application may not have write privileges\n2.) File may already be in use")));
        }
    }

    // ---------------------------------- Action Event Handlers ------------------------------------

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
        event.consume();
    }

    public void settingsAcceptButtonOnAction(ActionEvent event) {
        closeSettingsScreen(true);
        event.consume();
    }

    public void settingsCancelButtonOnAction(ActionEvent event) {
        closeSettingsScreen(false);
        event.consume();
    }

    public void lastFolderSettingBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose default save folder");
        if (lastFolderSettingTextField.getText() != null && !lastFolderSettingTextField.getText().isEmpty()) {
            File folder = new File(lastFolderSettingTextField.getText());
            if (folder.exists() && folder.isDirectory()) dc.setInitialDirectory(folder);
        }
        File result = dc.showDialog(settingsPane.getScene().getWindow());

        if (result != null) {
            lastFolderSettingTextField.setText(result.getAbsolutePath());
        }

        event.consume();
    }

    public void tagListExitButtonOnAction(ActionEvent event) {
        closeTagListScreen();
        event.consume();
    }

    public void helpExitButtonOnAction(ActionEvent event) {
        closeHelpScreen();
        event.consume();
    }

    public void progressLockStopButtonOnAction(ActionEvent event) {
        closeProgressLockScreen();
        event.consume();
    }

    public void duplicateLeftDeleteButtonOnAction(ActionEvent event) {
        if (currentlyPreviewingPair != null)
            deleteDuplicateImageEvent(currentlyPreviewingPair.getImg1(), currentlyPreviewingPair.getImg2());

        event.consume();
    }

    public void duplicateRightDeleteButtonOnAction(ActionEvent event) {
        if (currentlyPreviewingPair != null)
            deleteDuplicateImageEvent(currentlyPreviewingPair.getImg2(), currentlyPreviewingPair.getImg1());

        event.consume();
    }

    public void duplicateCloseButtonOnAction(ActionEvent event) {
        closeDuplicateScreen();
        event.consume();
    }

    public void duplicatePrevPairButtonOnAction(ActionEvent event) {
        previewLastSimilarPair();
        event.consume();
    }

    public void duplicateNextPairButtonOnAction(ActionEvent event) {
        previewNextSimilarPair();
        event.consume();
    }

    public void slideShowPreviousButtonOnAction(ActionEvent event) {
        slideShowShowPrevious();
        event.consume();
    }

    public void slideShowCloseButtonOnAction(ActionEvent event) {
        closeSlideShowScreen();
        event.consume();
    }

    public void slideShowNextButtonOnAction(ActionEvent event) {
        slideShowShowNext();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        requestImportFiles();
        event.consume();
    }

    public void importFolderMenuButtonOnAction(ActionEvent event) {
        requestImportFolder();
        event.consume();
    }

    public void settingsMenuButtonOnAction(ActionEvent event) {
        openSettingsScreen();
        event.consume();
    }

    public void helpMenuButtonOnAction(ActionEvent event) {
        openHelpScreen();
        event.consume();
    }

    public void slideShowSearchedMenuButtonOnAction(ActionEvent event) {
        openSlideShowScreen(currentSearch.getResults());
        event.consume();
    }

    public void slideShowSelectedMenuButtonOnAction(ActionEvent event) {
        openSlideShowScreen(imageGridView.getSelected());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        openTagListScreen();
        event.consume();
    }

    public void importFromFolderSettingBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose auto-import folder");
        if (importFromFolderSettingTextField.getText() != null && !importFromFolderSettingTextField.getText().isEmpty()) {
            File folder = new File(importFromFolderSettingTextField.getText());
            if (folder.exists() && folder.isDirectory()) dc.setInitialDirectory(folder);
        }
        File result = dc.showDialog(settingsPane.getScene().getWindow());

        if (result != null) {
            importFromFolderSettingTextField.setText(result.getAbsolutePath());
        }

        event.consume();
    }

    public void autoImportFolderSettingCheckBoxOnAction(ActionEvent event) {
        updateAutoImportFolderDisabledStatus();
        event.consume();
    }

    public void errorsPaneCloseButtonOnAction(ActionEvent event) {
        closeErrorsScreen();
        event.consume();
    }

    // ---------------------------------- Key Event Handlers ----------------------------------------

    public void explorerPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case F:
                    searchTextField.requestFocus();
                    event.consume();
                    break;
                case E:
                    editTagsTextField.setText(lastTagString);
                    editTagsTextField.requestFocus();
                    event.consume();
                    break;
                case Q:
                    menagerie.getUpdateQueue().enqueueUpdate(this::exit);
                    menagerie.getUpdateQueue().commit();
                    event.consume();
                    break;
                case S:
                    openSettingsScreen();
                    event.consume();
                    break;
                case T:
                    openTagListScreen();
                    event.consume();
                    break;
                case I:
                    if (event.isShiftDown())
                        requestImportFolder();
                    else
                        requestImportFiles();
                    event.consume();
                    break;
                case H:
                    openHelpScreen();
                    event.consume();
                    break;
                case D:
                    processAndShowDuplicates(imageGridView.getSelected());
                    event.consume();
                    break;
            }
        }

        switch (event.getCode()) {
            case ESCAPE:
                imageGridView.requestFocus();
                event.consume();
                break;
            case ALT:
                event.consume();
                break;
        }
    }

    public void explorerPaneOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ALT) {
            if (menuBar.isFocused()) {
                imageGridView.requestFocus();
            } else {
                menuBar.requestFocus();
            }
            event.consume();
        }
    }

    public void settingsPaneKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeSettingsScreen(false);
                event.consume();
                break;
            case ENTER:
                closeSettingsScreen(true);
                event.consume();
                break;
            case S:
                if (event.isControlDown()) {
                    closeSettingsScreen(false);
                    event.consume();
                }
                break;
        }
    }

    public void editTagsTextFieldOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case SPACE:
                editTagsOfSelected(editTagsTextField.getText());
                Platform.runLater(() -> editTagsTextField.setText(null));
                event.consume();
                break;
            case ENTER:
                editTagsOfSelected(editTagsTextField.getText());
                editTagsTextField.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
            case ESCAPE:
                editTagsTextField.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
        }
    }

    public void tagListPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeTagListScreen();
                event.consume();
                break;
            case T:
                if (event.isControlDown()) {
                    closeTagListScreen();
                    event.consume();
                }
                break;
        }
    }

    public void helpPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeHelpScreen();
                event.consume();
                break;
            case H:
                if (event.isControlDown()) {
                    closeHelpScreen();
                    event.consume();
                }
                break;
        }
    }

    public void progressLockPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeProgressLockScreen();
                event.consume();
                break;
        }
    }

    public void duplicatePaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeDuplicateScreen();
                event.consume();
                break;
            case LEFT:
                previewLastSimilarPair();
                event.consume();
                break;
            case RIGHT:
                previewNextSimilarPair();
                event.consume();
                break;
        }
    }

    public void slideShowPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case RIGHT:
                slideShowShowNext();
                event.consume();
                break;
            case LEFT:
                slideShowShowPrevious();
                event.consume();
                break;
            case ESCAPE:
                closeSlideShowScreen();
                event.consume();
                break;
        }
    }

    public void errorsPaneKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeErrorsScreen();
                event.consume();
                break;
        }
    }

    // --------------------------- Mouse Event Handlers -----------------------------------------------

    public void duplicateImagesPaneMouseEntered(MouseEvent event) {
        duplicateLeftTagListView.setDisable(false);
        duplicateRightTagListView.setDisable(false);
        duplicateLeftTagListView.setOpacity(0.75);
        duplicateRightTagListView.setOpacity(0.75);
        event.consume();
    }

    public void duplicateImagesPaneMouseExited(MouseEvent event) {
        duplicateLeftTagListView.setDisable(true);
        duplicateRightTagListView.setDisable(true);
        duplicateLeftTagListView.setOpacity(0);
        duplicateRightTagListView.setOpacity(0);
        event.consume();
    }

    public void dismissAllErrorsButtonOnAction(ActionEvent event) {
        errorsListView.getItems().clear();
        closeErrorsScreen();
        event.consume();
    }

    public void showErrorsButtonOnAction(ActionEvent event) {
        openErrorsScreen();
        event.consume();
    }

}
