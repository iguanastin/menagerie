package menagerie.gui;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menagerie.gui.errors.TrackedError;
import menagerie.gui.grid.ImageGridCell;
import menagerie.gui.grid.ImageGridView;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.media.DynamicVideoView;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.gui.screens.*;
import menagerie.gui.screens.importer.ImporterScreen;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.gui.thumbnail.VideoThumbnailThread;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.menagerie.history.TagEditEvent;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.model.search.Search;
import menagerie.model.search.SearchUpdateListener;
import menagerie.model.search.rules.*;
import menagerie.model.Settings;
import menagerie.util.CancellableThread;
import menagerie.util.Filters;
import menagerie.util.PokeListener;
import menagerie.util.folderwatcher.FolderWatcherThread;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

public class MainController {

    // ------------------------------- JFX -------------------------------------------
    public StackPane rootPane;

    public BorderPane explorerRootPane;
    public ToggleButton listDescendingToggleButton;
    public PredictiveTextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicMediaView previewMediaView;
    public Label resultCountLabel;
    public ItemInfoBox itemInfoBox;
    public ListView<Tag> tagListView;
    public PredictiveTextField editTagsTextField;
    public MenuBar menuBar;
    public Button showErrorsButton;
    public Button importsButton;

    public ScreenPane screenPane;

    // ----------------------------------- Screens -----------------------------------
    private TagListScreen tagListScreen;
    private HelpScreen helpScreen;
    private SlideshowScreen slideshowScreen;
    private ErrorsScreen errorsScreen;
    private DuplicateOptionsScreen duplicateOptionsScreen;
    private SettingsScreen settingsScreen;
    private ImporterScreen importerScreen;

    // --------------------------------- Menagerie vars ------------------------------
    private Menagerie menagerie;
    private ImporterThread importer;

    // ------------------------------- Explorer screen vars --------------------------
    private Search currentSearch = null;
    private ImageInfo currentlyPreviewing = null;
    private String lastEditTagString = null;
    private final ClipboardContent explorer_clipboard = new ClipboardContent();
    private boolean explorer_imageGridViewDragging = false;
    private ContextMenu explorer_cellContextMenu;
    private Stack<TagEditEvent> tagEditHistory = new Stack<>();

    // --------------------------------- Threads -------------------------------------
    private FolderWatcherThread folderWatcherThread = null;

    // ---------------------------------- Settings var -------------------------------
    private final Settings settings = new Settings(new File("menagerie.settings"));

    // ------------------------------ Video preview status ---------------------------
    private boolean playVideoAfterFocusGain = false;
    private boolean playVideoAfterExplorerEnabled = false;


    // ---------------------------------- Initializers -------------------------------

    @FXML
    public void initialize() {

        //Backup database
        if (settings.getBoolean(Settings.Key.BACKUP_DATABASE)) backupDatabase();

        //Initialize the menagerie
        initMenagerie();

        //Init screens
        initScreens();

        //Things to run on first "tick"
        Platform.runLater(() -> {
            //Apply window props and listeners
            initWindowPropertiesAndListeners();

            //Init closeRequest handling on window
            rootPane.getScene().getWindow().setOnCloseRequest(event -> cleanExit(false));
        });

        //Apply a default search
        applySearch(null, listDescendingToggleButton.isSelected());

        //Init folder watcher
        if (settings.getBoolean(Settings.Key.DO_AUTO_IMPORT)) startWatchingFolderForImages(settings.getString(Settings.Key.AUTO_IMPORT_FOLDER), settings.getBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT));

    }

    private void backupDatabase() {
        try {
            backUpDatabase(settings.getString(Settings.Key.DATABASE_URL));
        } catch (IOException e) {
            e.printStackTrace();
            Main.showErrorMessage("Error", "Error while trying to back up the database: " + settings.getString(Settings.Key.DATABASE_URL), e.getLocalizedMessage());
        }
    }

    private void initScreens() {
        initExplorerScreen();
        initSettingsScreen();
        initErrorsScreen();
        initTagListScreen();
        initSlideShowScreen();
        helpScreen = new HelpScreen();
        settingsScreen = new SettingsScreen(settings);
        duplicateOptionsScreen = new DuplicateOptionsScreen(settings);
        importerScreen = new ImporterScreen(importer, pairs -> duplicateOptionsScreen.getDuplicatesScreen().open(screenPane, menagerie, pairs), item -> imageGridView.select(item, false, false));
        importerScreen.getListView().getItems().addListener((ListChangeListener<? super ImportJob>) c -> Platform.runLater(() -> importsButton.setText("Imports: " + c.getList().size())));

        screenPane.getChildren().addListener((ListChangeListener<? super Node>) c -> explorerRootPane.setDisable(!c.getList().isEmpty())); //Init disable listener for explorer screen
    }

    private void initMenagerie() {
        try {
            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getString(Settings.Key.DATABASE_URL), settings.getString(Settings.Key.DATABASE_USER), settings.getString(Settings.Key.DATABASE_PASSWORD));
            DatabaseVersionUpdater.updateDatabase(db);

            menagerie = new Menagerie(db);
            importer = new ImporterThread(menagerie, settings);
            importer.setDaemon(true);
            importer.start();

            menagerie.getUpdateQueue().setErrorListener(e -> Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.HIGH, "Error while updating database", "An exception as thrown while trying to update the database", "Concurrent modification error or SQL statement out of date"))));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void initSlideShowScreen() {
        MenuItem showInSearchMenuItem = new MenuItem("Show in search");
        showInSearchMenuItem.setOnAction(event -> {
            slideshowScreen.close();
            imageGridView.select(slideshowScreen.getShowing(), false, false);
        });
        MenuItem forgetCurrentMenuItem = new MenuItem("Forget");
        forgetCurrentMenuItem.setOnAction(event -> slideshowScreen.tryDeleteCurrent(false));
        MenuItem deleteCurrentMenuItem = new MenuItem("Delete");
        deleteCurrentMenuItem.setOnAction(event -> slideshowScreen.tryDeleteCurrent(true));

        slideshowScreen = new SlideshowScreen();
        slideshowScreen.setItemContextMenu(new ContextMenu(showInSearchMenuItem, new SeparatorMenuItem(), forgetCurrentMenuItem, deleteCurrentMenuItem));
    }

    private void initErrorsScreen() {
        errorsScreen = new ErrorsScreen();
        errorsScreen.getErrors().addListener((ListChangeListener<? super TrackedError>) c -> {
            final int count = c.getList().size();

            if (count == 0) {
                showErrorsButton.setStyle("-fx-background-color: transparent;");
            } else {
                showErrorsButton.setStyle("-fx-background-color: red;");
            }

            showErrorsButton.setText("" + count);
        });
    }

    private void initSettingsScreen() {
        ((IntegerProperty) settings.getProperty(Settings.Key.GRID_WIDTH)).addListener((observable, oldValue, newValue) -> setGridWidth(newValue.intValue()));
        ((BooleanProperty) settings.getProperty(Settings.Key.DO_AUTO_IMPORT)).addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            // Defer to later to ensure other settings get updated before any action is taken, since this operation relies on other settings
            if (folderWatcherThread != null) {
                folderWatcherThread.stopWatching();
            }

            if (newValue)
                startWatchingFolderForImages(settings.getString(Settings.Key.AUTO_IMPORT_FOLDER), settings.getBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT));
        }));
        ((BooleanProperty) settings.getProperty(Settings.Key.MUTE_VIDEO)).addListener((observable, oldValue, newValue) -> previewMediaView.setMute(newValue));
        ((BooleanProperty) settings.getProperty(Settings.Key.REPEAT_VIDEO)).addListener((observable, oldValue, newValue) -> previewMediaView.setRepeat(newValue));
    }

    private void initTagListScreen() {
        tagListScreen = new TagListScreen();
        tagListScreen.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                MenuItem i1 = new MenuItem("Search this tag");
                i1.setOnAction(event1 -> {
                    searchTextField.setText(c.getItem().getName());
                    searchTextField.positionCaret(searchTextField.getText().length());
                    tagListScreen.close();
                    applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                });
                ContextMenu m = new ContextMenu(i1);
                m.show(c, event.getScreenX(), event.getScreenY());
            });
            return c;
        });
    }

    private void initExplorerScreen() {
        //Set image grid width from settings
        setGridWidth(settings.getInt(Settings.Key.GRID_WIDTH));

        //Init image grid
        imageGridView.setSelectionListener(image -> Platform.runLater(() -> previewItem(image)));
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
                            if (img.getThumbnail().isLoaded()) {
                                db.setDragView(img.getThumbnail().getImage());
                                break;
                            }
                        }
                    }

                    List<File> files = new ArrayList<>();
                    imageGridView.getSelected().forEach(img -> files.add(img.getFile()));
                    explorer_clipboard.putFiles(files);
                    db.setContent(explorer_clipboard);

                    explorer_imageGridViewDragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                explorer_imageGridViewDragging = false;
                event.consume();
            });
            c.setOnMouseReleased(event -> {
                if (!explorer_imageGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
                    imageGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                if (explorer_cellContextMenu.isShowing()) explorer_cellContextMenu.hide();
                explorer_cellContextMenu.show(c, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            return c;
        });
        imageGridView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                final boolean deleteFiles = !event.isControlDown();
                final PokeListener onFinish = () -> {
                    previewItem(null);
                    menagerie.removeImages(imageGridView.getSelected(), deleteFiles);
                };
                if (deleteFiles) {
                    new ConfirmationScreen().open(screenPane, "Delete files", "Permanently delete selected files? (" + imageGridView.getSelected().size() + " files)\n\n" +
                            "This action CANNOT be undone (files will be deleted)", onFinish, null);
                } else {
                    new ConfirmationScreen().open(screenPane, "Forget files", "Remove selected files from database? (" + imageGridView.getSelected().size() + " files)\n\n" +
                            "This action CANNOT be undone", onFinish, null);
                }
                event.consume();
            }
        });
        imageGridView.getSelected().addListener((ListChangeListener<? super ImageInfo>) c -> resultCountLabel.setText(imageGridView.getSelected().size() + " / " + currentSearch.getResults().size()));
        initExplorerGridCellContextMenu();

        //Init drag/drop handlers
        explorerRootPane.disabledProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (previewMediaView.isPlaying()) {
                    previewMediaView.pause();
                    playVideoAfterExplorerEnabled = true;
                }
            } else if (playVideoAfterExplorerEnabled) {
                previewMediaView.play();
                playVideoAfterExplorerEnabled = false;
            }
        });
        explorerRootPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        explorerRootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    importer.queue(new ImportJob(file, true, true));
                }
            } else if (url != null && !url.isEmpty()) {
                try {
                    importer.queue(new ImportJob(new URL(url), true, true));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
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
                        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                    });
                    MenuItem i2 = new MenuItem("Exclude from search");
                    i2.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> {
                        Map<ImageInfo, List<Tag>> removed = new HashMap<>();
                        imageGridView.getSelected().forEach(item -> {
                            if (item.removeTag(c.getItem())) {
                                removed.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
                            }
                        });

                        tagEditHistory.push(new TagEditEvent(null, removed));
                    });
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
            if (negative) {
                tags = new ArrayList<>();
                for (ImageInfo item : imageGridView.getSelected()) {
                    item.getTags().forEach(tag -> {
                        if (!tags.contains(tag)) tags.add(tag);
                    });
                }
            } else {
                tags = new ArrayList<>(menagerie.getTags());
            }

            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) {
                        results.add("-" + tag.getName());
                    } else {
                        results.add(tag.getName());
                    }
                }

                if (results.size() >= 8) break;
            }

            return results;
        });

        searchTextField.setTop(false);
        searchTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags = new ArrayList<>(menagerie.getTags());

            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) {
                        results.add("-" + tag.getName());
                    } else {
                        results.add(tag.getName());
                    }
                }

                if (results.size() >= 8) break;
            }

            return results;
        });

        previewMediaView.setMute(settings.getBoolean(Settings.Key.MUTE_VIDEO));
        previewMediaView.setRepeat(settings.getBoolean(Settings.Key.REPEAT_VIDEO));
    }

    private void initExplorerGridCellContextMenu() {
        MenuItem slideShowSelectedMenuItem = new MenuItem("Selected");
        slideShowSelectedMenuItem.setOnAction(event1 -> slideshowScreen.open(screenPane, menagerie, imageGridView.getSelected()));
        MenuItem slideShowSearchedMenuItem = new MenuItem("Searched");
        slideShowSearchedMenuItem.setOnAction(event1 -> slideshowScreen.open(screenPane, menagerie, imageGridView.getItems()));
        Menu slideShowMenu = new Menu("Slideshow", null, slideShowSelectedMenuItem, slideShowSearchedMenuItem);

        MenuItem openInExplorerMenuItem = new MenuItem("Open in Explorer");
        openInExplorerMenuItem.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + imageGridView.getLastSelected().getFile().getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Unexpected Error", "Error opening file explorer", e.getLocalizedMessage());
                }
            }
        });

        MenuItem buildMD5HashMenuItem = new MenuItem("Build MD5 Hash");
        buildMD5HashMenuItem.setOnAction(event1 -> {
            ProgressScreen ps = new ProgressScreen();
            CancellableThread ct = new CancellableThread() {
                @Override
                public void run() {
                    final int total = imageGridView.getSelected().size();
                    int i = 0;

                    for (ImageInfo item : imageGridView.getSelected()) {
                        if (!running) break;

                        i++;

                        if (item.getMD5() == null) {
                            try {
                                item.initializeMD5();
                                item.commitMD5ToDatabase();
                            } catch (Exception e) {
                                Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute MD5", "Exception was thrown while trying to compute an MD5 for file: " + item, "Unknown")));
                            }
                        }

                        final int finalI = i; // Lambda workaround
                        Platform.runLater(() -> ps.setProgress(finalI, total));
                    }

                    Platform.runLater(ps::close);
                }
            };
            ps.open(screenPane, "Building MD5s", "Building MD5 hashes for files...", ct::cancel);
            ct.start();
        });
        MenuItem buildHistogramMenuItem = new MenuItem("Build Histogram");
        buildHistogramMenuItem.setOnAction(event1 -> {
            ProgressScreen ps = new ProgressScreen();
            CancellableThread ct = new CancellableThread() {
                @Override
                public void run() {
                    final int total = imageGridView.getSelected().size();
                    int i = 0;

                    for (ImageInfo item : imageGridView.getSelected()) {
                        if (!running) break;

                        i++;

                        String filename = item.getFile().getName().toLowerCase();
                        if (item.getHistogram() == null && (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
                            try {
                                item.initializeHistogram();
                                item.commitHistogramToDatabase();
                            } catch (Exception e) {
                                Platform.runLater(() -> errorsScreen.addError(new TrackedError(e, TrackedError.Severity.NORMAL, "Failed to compute histogram", "Exception was thrown while trying to compute a histogram for image: " + item, "Unknown")));
                            }
                        }

                        final int finalI = i; // Lambda workaround
                        Platform.runLater(() -> ps.setProgress(finalI, total));
                    }

                    Platform.runLater(ps::close);
                }
            };
            ps.open(screenPane, "Building Histograms", "Building image histograms for selected files...", ct::cancel);
            ct.start();
        });

        MenuItem findDuplicatesMenuItem = new MenuItem("Find Duplicates");
        findDuplicatesMenuItem.setOnAction(event1 -> duplicateOptionsScreen.open(screenPane, menagerie, imageGridView.getSelected(), currentSearch.getResults(), menagerie.getItems()));

        MenuItem moveToFolderMenuItem = new MenuItem("Move To...");
        moveToFolderMenuItem.setOnAction(event1 -> {
            if (!imageGridView.getSelected().isEmpty()) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Move files to folder...");
                File result = dc.showDialog(rootPane.getScene().getWindow());

                if (result != null) {
                    ProgressScreen ps = new ProgressScreen();
                    CancellableThread ct = new CancellableThread() {
                        @Override
                        public void run() {
                            final int total = imageGridView.getSelected().size();
                            int i = 0;

                            for (ImageInfo item : imageGridView.getSelected()) {
                                if (!running) break;

                                i++;

                                File f = result.toPath().resolve(item.getFile().getName()).toFile();
                                if (!item.getFile().equals(f)) {
                                    File dest = MainController.resolveDuplicateFilename(f);

                                    if (!item.renameTo(dest)) {
                                        Platform.runLater(() -> errorsScreen.addError(new TrackedError(null, TrackedError.Severity.HIGH, "Error moving file", "An exception was thrown while trying to move a file\nFrom: " + item.getFile() + "\nTo: " + dest, "Unknown")));
                                    }
                                }

                                final int finalI = i; // Lambda workaround
                                Platform.runLater(() -> ps.setProgress(finalI, total));
                            }

                            Platform.runLater(ps::close);
                        }
                    };
                    ps.open(screenPane, "Moving files", "Moving files to: " + result.getAbsolutePath(), ct::cancel);
                    ct.start();
                }
            }
        });

        MenuItem removeImagesMenuItem = new MenuItem("Remove");
        removeImagesMenuItem.setOnAction(event1 -> new ConfirmationScreen().open(screenPane, "Forget files", "Remove selected files from database? (" + imageGridView.getSelected().size() + " files)\n\n" +
                "This action CANNOT be undone", () -> menagerie.removeImages(imageGridView.getSelected(), false), null));
        MenuItem deleteImagesMenuItem = new MenuItem("Delete");
        deleteImagesMenuItem.setOnAction(event1 -> new ConfirmationScreen().open(screenPane, "Delete files", "Permanently delete selected files? (" + imageGridView.getSelected().size() + " files)\n\n" +
                "This action CANNOT be undone (files will be deleted)", () -> {
            previewItem(null);
            menagerie.removeImages(imageGridView.getSelected(), true);
        }, null));

        explorer_cellContextMenu = new ContextMenu(slideShowMenu, new SeparatorMenuItem(), openInExplorerMenuItem, new SeparatorMenuItem(), buildMD5HashMenuItem, buildHistogramMenuItem, new SeparatorMenuItem(), findDuplicatesMenuItem, new SeparatorMenuItem(), moveToFolderMenuItem, new SeparatorMenuItem(), removeImagesMenuItem, deleteImagesMenuItem);
    }

    private void initWindowPropertiesAndListeners() {
        Stage stage = ((Stage) explorerRootPane.getScene().getWindow());
        stage.setMaximized(settings.getBoolean(Settings.Key.WINDOW_MAXIMIZED));
        if (settings.getInt(Settings.Key.WINDOW_WIDTH) > 0) {
            stage.setWidth(settings.getInt(Settings.Key.WINDOW_WIDTH));
        } else {
            settings.setInt(Settings.Key.WINDOW_WIDTH, (int) stage.getWidth());
        }
        if (settings.getInt(Settings.Key.WINDOW_HEIGHT) > 0) {
            stage.setHeight(settings.getInt(Settings.Key.WINDOW_HEIGHT));
        } else {
            settings.setInt(Settings.Key.WINDOW_HEIGHT, (int) stage.getHeight());
        }
        if (settings.getInt(Settings.Key.WINDOW_X) >= 0) {
            stage.setX(settings.getInt(Settings.Key.WINDOW_X));
        } else {
            settings.setInt(Settings.Key.WINDOW_X, (int) stage.getX());
        }
        if (settings.getInt(Settings.Key.WINDOW_Y) >= 0) {
            stage.setY(settings.getInt(Settings.Key.WINDOW_Y));
        } else {
            settings.setInt(Settings.Key.WINDOW_Y, (int) stage.getY());
        }

        //Bind window properties to settings
        stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.setBoolean(Settings.Key.WINDOW_MAXIMIZED, newValue));
        stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.setInt(Settings.Key.WINDOW_WIDTH, newValue.intValue()));
        stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.setInt(Settings.Key.WINDOW_HEIGHT, newValue.intValue()));
        stage.xProperty().addListener((observable, oldValue, newValue) -> settings.setInt(Settings.Key.WINDOW_X, newValue.intValue()));
        stage.yProperty().addListener((observable, oldValue, newValue) -> settings.setInt(Settings.Key.WINDOW_Y, newValue.intValue()));

        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (previewMediaView.isPlaying()) {
                    previewMediaView.pause();
                    playVideoAfterFocusGain = true;
                }
            } else if (playVideoAfterFocusGain) {
                previewMediaView.play();
                playVideoAfterFocusGain = false;
            }
        });
    }

    // -------------------------------- Dialog Openers ---------------------------------

    private void openImportFolderDialog() {
        DirectoryChooser dc = new DirectoryChooser();
        final String defaultFolder = settings.getString(Settings.Key.DEFAULT_FOLDER);
        if (defaultFolder != null && !defaultFolder.isEmpty())
            dc.setInitialDirectory(new File(defaultFolder));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            List<File> files = getFilesRecursively(result, Filters.FILE_NAME_FILTER);
            menagerie.getItems().forEach(img -> files.remove(img.getFile()));

            for (File file : files) {
                importer.queue(new ImportJob(file, true, true));
            }
        }
    }

    private void openImportFilesDialog() {
        FileChooser fc = new FileChooser();
        final String defaultFolder = settings.getString(Settings.Key.DEFAULT_FOLDER);
        if (defaultFolder != null && !defaultFolder.isEmpty())
            fc.setInitialDirectory(new File(defaultFolder));
        fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
        List<File> results = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (results != null && !results.isEmpty()) {
            final List<File> finalResults = new ArrayList<>(results);
            menagerie.getItems().forEach(img -> finalResults.remove(img.getFile()));

            for (File file : finalResults) {
                importer.queue(new ImportJob(file, true, true));
            }
        }
    }

    // ---------------------------------- GUI Action Methods ---------------------------

    private void previewItem(ImageInfo item) {
        if (currentlyPreviewing != null) currentlyPreviewing.setTagListener(null);
        currentlyPreviewing = item;

        if (!previewMediaView.preview(item)) {
            errorsScreen.addError(new TrackedError(null, TrackedError.Severity.NORMAL, "Unsupported preview filetype", "Tried to preview a filetype that isn't supposed", "An unsupported filetype somehow got added to the system"));
        }

        updateTagList(item);

        if (item != null) item.setTagListener(() -> updateTagList(item));
        itemInfoBox.setItem(item);
    }

    private void updateTagList(ImageInfo image) {
        tagListView.getItems().clear();
        if (image != null) {
            tagListView.getItems().addAll(image.getTags());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }
    }

    private void applySearch(String search, boolean descending) {
        if (currentSearch != null) currentSearch.close();
        previewItem(null);

        currentSearch = new Search(menagerie, constructRuleSet(search), descending);
        currentSearch.setListener(new SearchUpdateListener() {
            @Override
            public void imagesAdded(List<ImageInfo> images) {
                Platform.runLater(() -> {
                    imageGridView.getItems().addAll(0, images);

//                    imageGridView.getItems().sort(currentSearch.getComparator()); // This causes some gridcells to glitch out and get stuck visually
                });
            }

            @Override
            public void imagesRemoved(List<ImageInfo> images) {
                Platform.runLater(() -> {
                    final int oldLastIndex = imageGridView.getItems().indexOf(imageGridView.getLastSelected()) + 1;
                    int newIndex = oldLastIndex;
                    for (ImageInfo image : images) {
                        final int i = imageGridView.getItems().indexOf(image);
                        if (i < 0) continue;

                        if (i < oldLastIndex) {
                            newIndex--;
                        }
                    }

                    imageGridView.getItems().removeAll(images);
                    if (images.contains(currentlyPreviewing)) previewItem(null);

                    if (!imageGridView.getItems().isEmpty()) {
                        if (newIndex >= imageGridView.getItems().size())
                            newIndex = imageGridView.getItems().size() - 1;
                        imageGridView.setLastSelected(imageGridView.getItems().get(newIndex));
                    }
                });
            }
        });

        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(currentSearch.getResults());

        if (!imageGridView.getItems().isEmpty())
            imageGridView.select(imageGridView.getItems().get(0), false, false);
    }

    private List<SearchRule> constructRuleSet(String str) {
        if (str == null) str = "";
        List<SearchRule> rules = new ArrayList<>();
        for (String arg : str.split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            boolean inverted = false;
            if (arg.charAt(0) == '-') {
                inverted = true;
                arg = arg.substring(1);
            }

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
                    rules.add(new IDRule(type, Integer.parseInt(temp), inverted));
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
                    rules.add(new DateAddedRule(type, Long.parseLong(temp), inverted));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting long value for date added rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("md5:")) {
                rules.add(new MD5Rule(arg.substring(arg.indexOf(':') + 1), inverted));
            } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
                rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1), inverted));
            } else if (arg.startsWith("missing:")) {
                String type = arg.substring(arg.indexOf(':') + 1);
                switch (type.toLowerCase()) {
                    case "md5":
                        rules.add(new MissingRule(MissingRule.Type.MD5, inverted));
                        break;
                    case "file":
                        rules.add(new MissingRule(MissingRule.Type.FILE, inverted));
                        break;
                    case "histogram":
                    case "hist":
                        rules.add(new MissingRule(MissingRule.Type.HISTOGRAM, inverted));
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
                    rules.add(new TagCountRule(type, Integer.parseInt(temp), inverted));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for tag count rule", e.getLocalizedMessage());
                }
            } else {
                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, inverted));
            }
        }
        return rules;
    }

    private void setGridWidth(int n) {
        final double width = 18 + (Thumbnail.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * n;
        imageGridView.setMinWidth(width);
        imageGridView.setMaxWidth(width);
        imageGridView.setPrefWidth(width);
    }

    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || imageGridView.getSelected().isEmpty()) return;
        lastEditTagString = input.trim();

        if (imageGridView.getSelected().size() < 100) {
            editTagsUtility(input);
        } else {
            new ConfirmationScreen().open(screenPane, "Editting large number of items", "You are attempting to edit " + imageGridView.getSelected().size() + " items. Continue?", () -> editTagsUtility(input), null);
        }
    }

    private void editTagsUtility(String input) {
        List<ImageInfo> changed = new ArrayList<>();
        Map<ImageInfo, List<Tag>> added = new HashMap<>();
        Map<ImageInfo, List<Tag>> removed = new HashMap<>();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    for (ImageInfo item : imageGridView.getSelected()) {
                        if (item.removeTag(t)) {
                            changed.add(item);

                            removed.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                        }
                    }
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (ImageInfo item : imageGridView.getSelected()) {
                    if (item.addTag(t)) {
                        changed.add(item);

                        added.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                    }
                }
            }
        }

        if (!changed.isEmpty()) {
            menagerie.checkImagesStillValidInSearches(changed);

            tagEditHistory.push(new TagEditEvent(added, removed));
        }
    }

    private void startWatchingFolderForImages(String folder, boolean moveToDefault) {
        File watchFolder = new File(folder);
        if (watchFolder.exists() && watchFolder.isDirectory()) {
            folderWatcherThread = new FolderWatcherThread(watchFolder, Filters.FILE_NAME_FILTER, 30000, files -> {
                for (File file : files) {
                    if (moveToDefault) {
                        String work = settings.getString(Settings.Key.DEFAULT_FOLDER);
                        if (!work.endsWith("/") && !work.endsWith("\\")) work += "/";
                        File f = new File(work + file.getName());
                        if (file.equals(f)) continue; //File is being "moved" to same folder

                        File dest = resolveDuplicateFilename(f);

                        if (!file.renameTo(dest)) {
                            continue;
                        }

                        file = dest;
                    }

                    importer.queue(new ImportJob(file, true, true));
                }
            });
            folderWatcherThread.setDaemon(true);
            folderWatcherThread.start();
        }
    }

    private void cleanExit(boolean revertDatabase) {
        DynamicVideoView.releaseAllMediaPlayers();
        VideoThumbnailThread.releaseThreads();

        trySaveSettings();

        new Thread(() -> {
            try {
                System.out.println("Attempting to shut down Menagerie database and defragment the file");
                menagerie.getDatabase().createStatement().executeUpdate("SHUTDOWN DEFRAG;");
                System.out.println("Done defragging database file");

                if (revertDatabase) {
                    File database = getDatabaseFile(settings.getString(Settings.Key.DATABASE_URL));
                    File backup = new File(database + ".bak");
                    try {
                        Files.move(backup.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();

        Platform.exit();
    }

    // ---------------------------------- Compute Utilities -----------------------------

    private static List<File> getFilesRecursively(File folder, FileFilter filter) {
        File[] files = folder.listFiles();
        List<File> results = new ArrayList<>();
        if (files == null) return results;

        for (File file : files) {
            if (file.isDirectory()) {
                results.addAll(getFilesRecursively(file, filter));
            } else {
                if (filter.accept(file)) results.add(file);
            }
        }
        return results;
    }

    private static void backUpDatabase(String databaseURL) throws IOException {
        File dbFile = getDatabaseFile(databaseURL);

        if (dbFile.exists()) {
            System.out.println("Backing up database at: " + dbFile);
            File backupFile = new File(dbFile.getAbsolutePath() + ".bak");
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully backed up database to: " + backupFile);
        }
    }

    private static File getDatabaseFile(String databaseURL) {
        String path = databaseURL + ".mv.db";
        if (path.startsWith("~")) {
            String temp = System.getProperty("user.home");
            if (!temp.endsWith("/") && !temp.endsWith("\\")) temp += "/";
            path = path.substring(1);
            if (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);

            path = temp + path;
        }

        return new File(path);
    }

    public static File resolveDuplicateFilename(File file) {
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
            settings.save();
        } catch (IOException e1) {
            Platform.runLater(() -> errorsScreen.addError(new TrackedError(e1, TrackedError.Severity.HIGH, "Unable to save properties", "IO Exception thrown while trying to save properties file", "1.) Application may not have write privileges\n2.) File may already be in use")));
        }
    }

    // ---------------------------------- Action Event Handlers --------------------------

    public void searchButtonOnAction(ActionEvent event) {
        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
        imageGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        applySearch(searchTextField.getText(), listDescendingToggleButton.isSelected());
        imageGridView.requestFocus();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        openImportFilesDialog();
        event.consume();
    }

    public void importFolderMenuButtonOnAction(ActionEvent event) {
        openImportFolderDialog();
        event.consume();
    }

    public void settingsMenuButtonOnAction(ActionEvent event) {
        settingsScreen.open(screenPane);
        event.consume();
    }

    public void helpMenuButtonOnAction(ActionEvent event) {
        screenPane.open(helpScreen);
        event.consume();
    }

    public void viewSlideShowSearchedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, currentSearch.getResults());
        event.consume();
    }

    public void viewSlideShowSelectedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, imageGridView.getSelected());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        tagListScreen.open(screenPane, menagerie.getTags());
        event.consume();
    }

    public void showErrorsButtonOnAction(ActionEvent event) {
        screenPane.open(errorsScreen);
        event.consume();
    }

    public void revertDatabaseMenuButtonOnAction(ActionEvent event) {
        File database = getDatabaseFile(settings.getString(Settings.Key.DATABASE_URL));
        File backup = new File(database + ".bak");
        if (backup.exists()) {
            new ConfirmationScreen().open(screenPane, "Revert database", "Revert to latest backup? (" + new Date(backup.lastModified()) + ")\n\nLatest backup: \"" + backup + "\"\n\nNote: Files will not be deleted!", () -> cleanExit(true), null);
        }
        event.consume();
    }

    public void importsButtonOnAction(ActionEvent event) {
        screenPane.open(importerScreen);
        event.consume();
    }

    // ---------------------------------- Key Event Handlers -------------------------------

    public void explorerRootPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case F:
                    searchTextField.requestFocus();
                    event.consume();
                    break;
                case E:
                    editTagsTextField.setText(lastEditTagString);
                    editTagsTextField.requestFocus();
                    event.consume();
                    break;
                case Q:
                    menagerie.getUpdateQueue().enqueueUpdate(() -> cleanExit(false));
                    menagerie.getUpdateQueue().commit();
                    event.consume();
                    break;
                case S:
                    settingsScreen.open(screenPane);
                    event.consume();
                    break;
                case T:
                    tagListScreen.open(screenPane, menagerie.getTags());
                    event.consume();
                    break;
                case I:
                    if (event.isShiftDown())
                        openImportFolderDialog();
                    else
                        openImportFilesDialog();
                    event.consume();
                    break;
                case H:
                    screenPane.open(helpScreen);
                    event.consume();
                    break;
                case D:
                    duplicateOptionsScreen.open(screenPane, menagerie, imageGridView.getSelected(), currentSearch.getResults(), menagerie.getItems());
                    event.consume();
                    break;
                case Z:
                    if (tagEditHistory.empty()) {
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        TagEditEvent peek = tagEditHistory.peek();
                        new ConfirmationScreen().open(screenPane, "Undo last tag edit?", "Tags were added to " + peek.getAdded().keySet().size() + " items.\nTags were removed from " + peek.getRemoved().keySet().size() + " others.", () -> {
                            TagEditEvent pop = tagEditHistory.pop();
                            pop.reverseAction();

                            List<ImageInfo> list = new ArrayList<>();
                            pop.getAdded().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            pop.getRemoved().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            menagerie.checkImagesStillValidInSearches(list);
                        }, null);
                    }
                    event.consume();
                    break;
                case N:
                    screenPane.open(importerScreen);
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

    public void explorerRootPaneOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ALT) {
            if (menuBar.isFocused()) {
                imageGridView.requestFocus();
            } else {
                menuBar.requestFocus();
            }
            event.consume();
        }
    }

    public void editTagsTextFieldOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
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

}
