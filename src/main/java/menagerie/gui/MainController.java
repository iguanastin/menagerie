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
import menagerie.gui.grid.ItemGridCell;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.gui.screens.*;
import menagerie.gui.screens.dialogs.*;
import menagerie.gui.screens.duplicates.DuplicateOptionsScreen;
import menagerie.gui.screens.importer.ImporterScreen;
import menagerie.gui.screens.log.LogItem;
import menagerie.gui.screens.log.LogListCell;
import menagerie.gui.screens.log.LogScreen;
import menagerie.gui.taglist.TagListCell;
import menagerie.gui.taglist.TagListPopup;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.Settings;
import menagerie.model.menagerie.*;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.model.search.GroupSearch;
import menagerie.model.search.Search;
import menagerie.model.search.SearchHistory;
import menagerie.util.CancellableThread;
import menagerie.util.Filters;
import menagerie.util.folderwatcher.FolderWatcherThread;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class MainController {

    // ------------------------------- JFX -------------------------------------------
    public StackPane rootPane;

    public BorderPane explorerRootPane;
    public MenuBar menuBar;
    public PredictiveTextField searchTextField;
    public PredictiveTextField editTagsTextField;
    public ToggleButton listDescendingToggleButton;
    public ToggleButton showGroupedToggleButton;
    public ItemGridView itemGridView;
    public DynamicMediaView previewMediaView;
    public ItemInfoBox itemInfoBox;
    public ListView<Tag> tagListView;
    public Label resultCountLabel;
    public Label explorerZoomLabel;
    public Label scopeLabel;
    public Button importsButton;
    public Button logButton;
    public Button backButton;

    public ScreenPane screenPane;

    // ----------------------------------- Screens -----------------------------------
    private TagListScreen tagListScreen;
    private HelpScreen helpScreen;
    private SlideshowScreen slideshowScreen;
    private SettingsScreen settingsScreen;
    private ImporterScreen importerScreen;
    private LogScreen logScreen;
    private DuplicateOptionsScreen duplicateOptionsScreen;
    private ImportDialogScreen importDialogScreen;
    private GroupDialogScreen groupDialogScreen;

    // --------------------------------- Menagerie vars ------------------------------
    /**
     * The menagerie this application is using.
     */
    private Menagerie menagerie;
    /**
     * History of tag edit events.
     */
    private final Stack<TagEditEvent> tagEditHistory = new Stack<>();

    // ------------------------------- Explorer screen vars --------------------------
    /**
     * Clipboard content object used by this application.
     */
    private final ClipboardContent clipboard = new ClipboardContent();
    /**
     * Current search that is active and being shown in the item grid.
     */
    private Search currentSearch = null;
    /**
     * Item that is currently being displayed in the preview viewport.
     */
    private Item currentlyPreviewing = null;
    /**
     * The last string that was used to edit tags.
     */
    private String lastEditTagString = null;
    /**
     * Variable used to track drag status of items from the item grid.
     */
    private boolean itemGridViewDragging = false;
    /**
     * Search history stack
     */
    private final Stack<SearchHistory> searchHistory = new Stack<>();
    private final ListChangeListener<Tag> previewTagListener = c -> {
        while (c.next()) {
            tagListView.getItems().addAll(c.getAddedSubList());
            tagListView.getItems().removeAll(c.getRemoved());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }
    };
    private final ListChangeListener<Item> searchChangeListener = c -> {
        while (c.next()) {
            // Added
            if (c.wasAdded()) {
                itemGridView.getItems().addAll(0, c.getAddedSubList()); // TODO: Insert these in the right position or sort it or something.
            }

            // Removed
            if (c.wasRemoved()) {
                if (c.getRemoved().contains(currentlyPreviewing)) previewItem(null);

                final int oldLastIndex = itemGridView.getItems().indexOf(itemGridView.getLastSelected()) + 1;
                int newIndex = oldLastIndex;
                for (Item image : c.getRemoved()) {
                    final int i = itemGridView.getItems().indexOf(image);
                    if (i < 0) continue;

                    if (i < oldLastIndex) {
                        newIndex--;
                    }
                }

                itemGridView.getItems().removeAll(c.getRemoved());

                if (!itemGridView.getItems().isEmpty()) {
                    if (newIndex >= itemGridView.getItems().size()) newIndex = itemGridView.getItems().size() - 1;
                    if (newIndex >= 0) itemGridView.select(itemGridView.getItems().get(newIndex), false, false);
                }
            }
        }
    };

    // --------------------------------- Threads -------------------------------------
    /**
     * Importer thread for the menagerie. Main pipeline for adding any new items.
     */
    private ImporterThread importer;
    /**
     * Current folder watcher thread, may be null. Thread monitors a folder for new files and sends them to the importer.
     */
    private FolderWatcherThread folderWatcherThread = null;

    // ---------------------------------- Settings var -------------------------------
    /**
     * Settings object used by this application.
     */
    private final Settings settings;

    // ------------------------------ Video preview status ---------------------------
    /**
     * Variable used to track if a video should be played after the player regains focus.
     */
    private boolean playVideoAfterFocusGain = false;
    /**
     * Variable used to track if a video should be played after the explorer regains focus.
     */
    private boolean playVideoAfterExplorerEnabled = false;


    // --------------------------------- Constructor ---------------------------------

    public MainController(Menagerie menagerie, Settings settings) {
        this.menagerie = menagerie;
        this.settings = settings;

        if (settings.getDouble(Settings.Key.CONFIDENCE) < 0.9) settings.setDouble(Settings.Key.CONFIDENCE, 0.9);
    }

    // ---------------------------------- Initializers -------------------------------

    /**
     * Initializes this controller and elements
     */
    @FXML
    public void initialize() {

        // Initialize the menagerie
        initImporterThread();

        // Init screens
        initScreens();

        // Things to run on first "tick"
        Platform.runLater(() -> {
            //Apply window props and listeners
            initWindowPropertiesAndListeners();

            //Init closeRequest handling on window
            rootPane.getScene().getWindow().setOnCloseRequest(event -> cleanExit(false));
        });

        // Apply a default search
        applySearch(null, null, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());

        // Init folder watcher
        if (settings.getBoolean(Settings.Key.DO_AUTO_IMPORT))
            startWatchingFolderForImages(settings.getString(Settings.Key.AUTO_IMPORT_FOLDER), settings.getBoolean(Settings.Key.AUTO_IMPORT_MOVE_TO_DEFAULT));

        // Show help screen if setting is set
        if (settings.getBoolean(Settings.Key.SHOW_HELP_ON_START)) {
            screenPane.open(helpScreen);
            settings.setBoolean(Settings.Key.SHOW_HELP_ON_START, false);
        }

    }

    /**
     * Initializes JFX screen objects for the ScreenPane
     */
    private void initScreens() {
        Main.log.info("Initializing screens");

        initExplorer();
        initSettingsScreen();
        initTagListScreen();
        slideshowScreen = new SlideshowScreen(item -> {
            slideshowScreen.close();
            itemGridView.select(item, false, false);
        });
        helpScreen = new HelpScreen();
        settingsScreen = new SettingsScreen(settings);
        duplicateOptionsScreen = new DuplicateOptionsScreen(settings);
        duplicateOptionsScreen.getDuplicatesScreen().setSelectListener(item -> itemGridView.select(item, false, false));
        importerScreen = new ImporterScreen(importer, pairs -> duplicateOptionsScreen.getDuplicatesScreen().open(screenPane, menagerie, pairs), item -> itemGridView.select(item, false, false), count -> {
            Platform.runLater(() -> importsButton.setText("Imports: " + count));

            if (count == 0) {
                importsButton.setStyle(null);
            } else {
                importsButton.setStyle("-fx-base: blue;");
            }
        });
        initLogScreen();
        importDialogScreen = new ImportDialogScreen(settings, menagerie, importer);
        groupDialogScreen = new GroupDialogScreen();

        screenPane.getChildren().addListener((ListChangeListener<? super Node>) c -> explorerRootPane.setDisable(!c.getList().isEmpty())); //Init disable listener for explorer screen
    }

    /**
     * Initializes menagerie importer thread.
     */
    private void initImporterThread() {
        //        try {
        //            Main.log.info("Connecting to database: " + settings.getString(Settings.Key.DATABASE_URL) + " - " + settings.getString(Settings.Key.DATABASE_USER) + "/" + settings.getString(Settings.Key.DATABASE_PASSWORD));
        //            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getString(Settings.Key.DATABASE_URL), settings.getString(Settings.Key.DATABASE_USER), settings.getString(Settings.Key.DATABASE_PASSWORD));
        //            Main.log.info("Verifying/updating database");
        //            DatabaseVersionUpdater.updateDatabase(db);
        //
        //            DatabaseManager dbManager = new DatabaseManager(db);
        //            dbManager.setDaemon(true);
        //            dbManager.start();
        //
        //            Main.log.info("Initializing Menagerie");
        //            menagerie = new Menagerie(dbManager);

        Main.log.info("Starting importer thread");
        importer = new ImporterThread(menagerie, settings);
        importer.setDaemon(true);
        importer.start();
        //        } catch (SQLException e) {
        //            Main.log.log(Level.SEVERE, "Error connecting to or verifying database", e);
        //            Main.showErrorMessage("Database Error", "Error when connecting to or verifying database", e.getLocalizedMessage());
        //            Platform.exit();
        //        }
    }

    /**
     * Initializes the settings screen
     */
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

    /**
     * Initializes the tag list screen
     */
    private void initTagListScreen() {
        tagListScreen = new TagListScreen();
        tagListScreen.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                MenuItem i0 = new MenuItem("Add note");
                i0.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note", String.format("Add a note to tag '%s'", c.getItem().getName()), null, note -> c.getItem().addNote(note), null));
                MenuItem i1 = new MenuItem("Search this tag");
                i1.setOnAction(event1 -> {
                    searchTextField.setText(c.getItem().getName());
                    searchTextField.positionCaret(searchTextField.getText().length());
                    tagListScreen.close();
                    GroupItem scope = null;
                    if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
                    applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
                });
                ContextMenu m = new ContextMenu(i0, new SeparatorMenuItem(), i1);
                m.show(c, event.getScreenX(), event.getScreenY());
            });
            final TagListPopup popup = new TagListPopup();
            popup.setAutoHide(true);
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && c.getItem() != null) {
                    popup.setTag(c.getItem());
                    popup.show(c, event.getScreenX(), event.getScreenY());
                }
            });
            return c;
        });
    }

    /**
     * Initializes the log screen
     */
    private void initLogScreen() {
        logScreen = new LogScreen();
        logScreen.getListView().setCellFactory(param -> new LogListCell());
        Main.log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                StringBuilder work = new StringBuilder(new Date(record.getMillis()).toString());
                work.append(" [").append(record.getLevel()).append("]: ").append(record.getMessage());
                if (record.getThrown() != null) {
                    work.append("\n").append(record.getThrown().toString());
                    for (StackTraceElement e : record.getThrown().getStackTrace()) {
                        work.append("\n    at ").append(e);
                    }
                }
                Platform.runLater(() -> {
                    LogItem item;
                    if (record.getLevel() == Level.SEVERE) {
                        item = new LogItem(work.toString(), "-fx-text-fill: red;");
                        logButton.setStyle("-fx-base: red;");
                    } else if (record.getLevel() == Level.WARNING) {
                        item = new LogItem(work.toString(), "-fx-text-fill: yellow;");
                        logButton.setStyle("-fx-base: yellow;");
                    } else {
                        item = new LogItem(work.toString());
                    }
                    logScreen.getListView().getItems().add(item);
                    if (logScreen.getListView().getItems().size() > 1000) logScreen.getListView().getItems().remove(0);
                });
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });
    }

    /**
     * Initializes the explorer
     */
    private void initExplorer() {
        // Set image grid width from settings
        setGridWidth(settings.getInt(Settings.Key.GRID_WIDTH));

        // Init image grid
        initItemGridView();

        // Init drag/drop handlers
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
                event.consume();
            }
        });
        explorerRootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    importer.addJob(new ImportJob(file));
                }
            } else if (url != null && !url.isEmpty()) {
                try {
                    String folder = settings.getString(Settings.Key.DEFAULT_FOLDER);
                    String filename = new URL(url).getPath().replaceAll("^.*/", "");
                    File target;
                    if (!settings.getBoolean(Settings.Key.USE_FILENAME_FROM_URL) || folder == null || folder.isEmpty() || !Files.isDirectory(Paths.get(folder))) {
                        do {
                            FileChooser fc = new FileChooser();
                            fc.setTitle("Save as");
                            fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
                            if (folder != null && !folder.isEmpty()) fc.setInitialDirectory(new File(folder));
                            fc.setInitialFileName(filename);

                            target = fc.showSaveDialog(rootPane.getScene().getWindow());

                            if (target == null) return;
                        } while (target.exists() || !target.getParentFile().exists());
                    } else {
                        target = resolveDuplicateFilename(new File(folder, filename));
                    }
                    importer.addJob(new ImportJob(new URL(url), target));
                } catch (MalformedURLException e) {
                    Main.log.log(Level.WARNING, "File dragged from web has bad URL", e);
                }
            }
            event.consume();
        });

        // Init tag list cell factory
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    MenuItem i0 = new MenuItem("Add note");
                    i0.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note", String.format("Add a note to tag '%s'", c.getItem().getName()), null, note -> c.getItem().addNote(note), null));
                    MenuItem i1 = new MenuItem("Add to search");
                    i1.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " " + c.getItem().getName());
                        GroupItem scope = null;
                        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
                        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
                    });
                    MenuItem i2 = new MenuItem("Exclude from search");
                    i2.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        GroupItem scope = null;
                        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
                        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> {
                        Map<Item, List<Tag>> removed = new HashMap<>();
                        itemGridView.getSelected().forEach(item -> {
                            if (item.removeTag(c.getItem())) {
                                removed.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
                            }
                        });

                        tagEditHistory.push(new TagEditEvent(null, removed));
                    });
                    ContextMenu m = new ContextMenu(i0, new SeparatorMenuItem(), i1, i2, new SeparatorMenuItem(), i3);
                    m.show(c, event.getScreenX(), event.getScreenY());
                }
            });

            final TagListPopup popup = new TagListPopup();
            popup.setAutoHide(true);
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && c.getItem() != null) {
                    popup.setTag(c.getItem());
                    popup.show(c, event.getScreenX(), event.getScreenY());
                }
            });

            return c;
        });

        // Init predictive textfields
        editTagsTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags;
            if (negative) {
                tags = new ArrayList<>();
                for (Item item : itemGridView.getSelected()) {
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

        // Init preview
        previewMediaView.getMediaView().getScale().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() == 1) {
                explorerZoomLabel.setText(null);
            } else {
                explorerZoomLabel.setText(String.format("%d%%", (int) (100 * (1 / newValue.doubleValue()))));
            }
        });
        previewMediaView.setMute(settings.getBoolean(Settings.Key.MUTE_VIDEO));
        previewMediaView.setRepeat(settings.getBoolean(Settings.Key.REPEAT_VIDEO));
    }

    /**
     * Initializes listeners and etc. of the itemGridView
     */
    private void initItemGridView() {
        itemGridView.addSelectionListener(image -> Platform.runLater(() -> previewItem(image)));
        itemGridView.setCellFactory(param -> {
            ItemGridCell c = new ItemGridCell();
            c.setOnDragDetected(event -> {
                if (!itemGridView.getSelected().isEmpty() && event.isPrimaryButtonDown()) {
                    if (c.getItem() instanceof MediaItem && !itemGridView.isSelected(c.getItem()))
                        itemGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());

                    Dragboard db = c.startDragAndDrop(TransferMode.ANY);

                    for (Item item : itemGridView.getSelected()) {
                        if (item instanceof MediaItem) {
                            String filename = ((MediaItem) item).getFile().getName().toLowerCase();
                            if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")) {
                                if (item.getThumbnail().isLoaded()) {
                                    db.setDragView(item.getThumbnail().getImage());
                                    break;
                                }
                            }
                        }
                    }

                    List<File> files = new ArrayList<>();
                    itemGridView.getSelected().forEach(item -> {
                        if (item instanceof MediaItem) files.add(((MediaItem) item).getFile());
                        else if (item instanceof GroupItem)
                            ((GroupItem) item).getElements().forEach(mediaItem -> files.add(mediaItem.getFile()));
                    });
                    clipboard.putFiles(files);
                    db.setContent(clipboard);

                    itemGridViewDragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                itemGridViewDragging = false;
                event.consume();
            });
            c.setOnDragOver(event -> {
                if (event.getGestureSource() instanceof ItemGridCell && currentSearch instanceof GroupSearch && !event.getGestureSource().equals(c)) {
                    event.acceptTransferModes(TransferMode.ANY);
                }
            });
            c.setOnDragDropped(event -> {
                if (!itemGridView.getSelected().isEmpty()) {
                    List<MediaItem> list = new ArrayList<>();
                    itemGridView.getSelected().forEach(item -> list.add((MediaItem) item));
                    list.sort(Comparator.comparingInt(MediaItem::getPageIndex));

                    boolean before = false;
                    if (c.sceneToLocal(event.getSceneX(), event.getSceneY()).getX() < Thumbnail.THUMBNAIL_SIZE / 2)
                        before = true;
                    if (currentSearch.isDescending()) before = !before;
                    if (((MediaItem) c.getItem()).getGroup().moveElements(list, (MediaItem) c.getItem(), before)) {
                        currentSearch.sort();
                        itemGridView.getItems().sort(currentSearch.getComparator());
                        event.consume();
                    }
                }
            });
            c.setOnMouseReleased(event -> {
                if (!itemGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
                    itemGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                if (!itemGridView.isSelected(c.getItem())) {
                    itemGridView.select(c.getItem(), false, false);
                }
                constructGridCellContextMenu(itemGridView.getSelected()).show(c, event.getScreenX(), event.getScreenY());
                event.consume();
            });
            c.setOnMouseClicked(event -> {
                if (c.getItem() instanceof GroupItem && event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
                    explorerOpenGroup((GroupItem) c.getItem());
                }
            });
            return c;
        });
        itemGridView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                if (event.isControlDown()) {
                    forgetFilesDialog(itemGridView.getSelected());
                } else {
                    deleteFilesDialog(itemGridView.getSelected());
                }
                event.consume();
            } else if (event.getCode() == KeyCode.G && event.isControlDown()) {
                groupDialog(itemGridView.getSelected());
            } else if (event.getCode() == KeyCode.U && event.isControlDown()) {
                ungroupDialog(itemGridView.getSelected());
            }
        });
        itemGridView.getSelected().addListener((ListChangeListener<? super Item>) c -> resultCountLabel.setText(itemGridView.getSelected().size() + " / " + currentSearch.getResults().size()));
        itemGridView.getItems().addListener((ListChangeListener<? super Item>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    itemGridView.getItems().sort(currentSearch.getComparator());
                    break;
                }
            }
        });
    }

    /**
     * Applies window properties to the window and starts listeners for changes to update the settings object.
     */
    private void initWindowPropertiesAndListeners() {
        Main.log.info("Initializing window properties and listeners");

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

    // ---------------------------------- GUI Action Methods ---------------------------

    /**
     * Constructs a context menu for a given set of items. Different combinations of GroupItem and MediaItem will give different context menu items.
     *
     * @param selected Set of items that this context menu with operate on.
     * @return A context menu ready to be shown for the given set of items.
     */
    private ContextMenu constructGridCellContextMenu(List<Item> selected) {
        if (selected == null || selected.isEmpty()) {
            MenuItem mi = new MenuItem("Nothing selected");
            mi.setDisable(true);
            return new ContextMenu(mi);
        }

        ContextMenu cm = new ContextMenu();

        int groupCount = 0, mediaCount = 0, itemsInGroupCount = 0;
        for (Item item : selected) {
            if (item instanceof GroupItem) {
                groupCount++;
            } else if (item instanceof MediaItem) {
                mediaCount++;
                if (((MediaItem) item).isInGroup()) itemsInGroupCount++;
            }
        }

        // More than one group or at least one media item
        if (groupCount == 1 && selected.size() == 1) {
            MenuItem elementTags = new MenuItem("Sync element tags to group");
            elementTags.setOnAction(event -> {
                GroupItem group = (GroupItem) selected.get(0);
                group.getElements().forEach(item -> item.getTags().forEach(group::addTag));
            });
            cm.getItems().add(elementTags);
            MenuItem reverse = new MenuItem("Reverse element order");
            reverse.setOnAction(event -> ((GroupItem) selected.get(0)).reverseElements());
            cm.getItems().add(reverse);
            MenuItem rename = new MenuItem("Rename group");
            rename.setOnAction(event -> {
                String title = ((GroupItem) selected.get(0)).getTitle();
                new TextDialogScreen().open(screenPane, "Rename group", "Current: " + title, title, newTitle -> {
                    ((GroupItem) selected.get(0)).setTitle(newTitle);
                }, null);
            });
            cm.getItems().add(rename);
            cm.getItems().add(new SeparatorMenuItem());
        }
        if (groupCount > 1 || mediaCount > 0) {
            MenuItem combineGroups = new MenuItem("Combine into Group");
            combineGroups.setOnAction(event -> groupDialog(selected));
            cm.getItems().add(combineGroups);
        }
        if (itemsInGroupCount > 0) {
            MenuItem removeFromGroup = new MenuItem("Remove from group");
            removeFromGroup.setOnAction(event -> selected.forEach(item -> {
                if (item instanceof MediaItem && ((MediaItem) item).isInGroup()) {
                    ((MediaItem) item).getGroup().removeItem((MediaItem) item);
                }
            }));
            cm.getItems().add(removeFromGroup);
        }

        if (groupCount > 0 || mediaCount > 0) {
            Menu slideshow = new Menu("Slideshow...");
            MenuItem grabbed = new MenuItem("Selected");
            grabbed.setOnAction(event -> openSlideShow(selected));
            MenuItem searched = new MenuItem("Searched");
            searched.setOnAction(event -> openSlideShow(currentSearch.getResults()));
            MenuItem all = new MenuItem("All");
            all.setOnAction(event -> openSlideShow(menagerie.getItems()));
            slideshow.getItems().addAll(grabbed, searched, all);

            MenuItem moveFiles = new MenuItem("Move Files");
            moveFiles.setOnAction(event -> moveFilesDialog(selected));

            MenuItem findDupes = new MenuItem("Find Duplicates");
            findDupes.setOnAction(event -> duplicateOptionsScreen.open(screenPane, menagerie, selected, currentSearch.getResults(), menagerie.getItems()));

            cm.getItems().addAll(slideshow, moveFiles, findDupes);
        }

        if (mediaCount > 0) {
            MenuItem openDefault = new MenuItem("Open");
            openDefault.setOnAction(event -> {
                Item last = selected.get(selected.size() - 1);
                if (last instanceof MediaItem) {
                    try {
                        Desktop.getDesktop().open(((MediaItem) last).getFile());
                    } catch (IOException e) {
                        Main.log.log(Level.WARNING, "Error opening file with system default program", e);
                    }
                }
            });
            MenuItem explorer = new MenuItem("Open in Explorer");
            explorer.setOnAction(event -> {
                Item last = selected.get(selected.size() - 1);
                if (last instanceof MediaItem) {
                    try {
                        Runtime.getRuntime().exec("explorer.exe /select, " + ((MediaItem) last).getFile().getAbsolutePath());
                    } catch (IOException e) {
                        Main.log.log(Level.SEVERE, "Error opening file in explorer", e);
                    }
                }
            });
            cm.getItems().addAll(openDefault, explorer);
        }

        if (groupCount > 0 || mediaCount > 0) {
            cm.getItems().add(new SeparatorMenuItem());
            if (groupCount > 0) {
                MenuItem ungroup = new MenuItem("Ungroup");
                ungroup.setOnAction(event -> ungroupDialog(selected));
                cm.getItems().add(ungroup);
            }
            MenuItem forget = new MenuItem("Forget files");
            forget.setOnAction(event -> forgetFilesDialog(selected));
            forget.setStyle("-fx-text-fill: red;");
            MenuItem delete = new MenuItem("Delete files");
            delete.setOnAction(event -> deleteFilesDialog(selected));
            delete.setStyle("-fx-text-fill: red;");
            cm.getItems().addAll(forget, delete);
        }

        return cm;
    }

    /**
     * Attempts to display an item's media in the preview viewport.
     *
     * @param item The item to display. Displays nothing when item is a GroupItem.
     */
    private void previewItem(Item item) {
        if (currentlyPreviewing != null) currentlyPreviewing.getTags().removeListener(previewTagListener);
        currentlyPreviewing = item;

        if (item instanceof MediaItem) {
            if (!previewMediaView.preview((MediaItem) item)) {
                Main.log.warning("Failed to preview file: " + ((MediaItem) item).getFile());
            }
            itemInfoBox.setItem((MediaItem) item);
        } else if (item instanceof GroupItem) {
            previewMediaView.preview(((GroupItem) item).getElements().get(0));
            itemInfoBox.setItem(((GroupItem) item).getElements().get(0));
        }

        tagListView.getItems().clear();
        if (item != null) {
            tagListView.getItems().addAll(item.getTags());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }

        if (item != null) item.getTags().addListener(previewTagListener);
    }

    /**
     * Opens a dialog asking for user confirmation to forget files from the menagerie without deleting the file.
     *
     * @param toForget Set of items to forget if user confirms.
     */
    private void forgetFilesDialog(List<Item> toForget) {
        List<Item> items = new ArrayList<>(toForget);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) items.addAll(((GroupItem) items.get(i)).getElements());
        }
        if (currentlyPreviewing instanceof MediaItem && items.contains(currentlyPreviewing) && ((MediaItem) currentlyPreviewing).isVideo())
            previewMediaView.stop();
        new ConfirmationScreen().open(screenPane, "Forget files", String.format("Remove selected files from database? (%d files)\n\n" + "This action CANNOT be undone", items.size()), () -> menagerie.forgetItems(items), null);
    }

    /**
     * Opens a dialog asking for user confirmation to delete files from the menagerie. Will delete files from the local disk.
     *
     * @param toDelete Set of items to forget and delete if user confirms.
     */
    private void deleteFilesDialog(List<Item> toDelete) {
        List<Item> items = new ArrayList<>(toDelete);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) items.addAll(((GroupItem) items.get(i)).getElements());
        }
        if (currentlyPreviewing instanceof MediaItem && items.contains(currentlyPreviewing) && ((MediaItem) currentlyPreviewing).isVideo())
            previewMediaView.stop();
        new ConfirmationScreen().open(screenPane, "Delete files", String.format("Permanently delete selected files? (%d files)\n\n" + "This action CANNOT be undone (files will be deleted)", items.size()), () -> menagerie.deleteItems(items), null);
    }

    /**
     * Opens a dialog asking for user confirmation to ungroup given groups. Items that are not a GroupItem will be ignored.
     *
     * @param items Set of items to ungroup if user confirms.
     */
    private void ungroupDialog(List<Item> items) {
        List<Item> groups = new ArrayList<>(items);
        groups.removeIf(item -> !(item instanceof GroupItem));
        new ConfirmationScreen().open(screenPane, "Ungroup group?", String.format("Are you sure you want to ungroup %d groups?", groups.size()), () -> menagerie.forgetItems(groups), null);
    }

    /**
     * Opens a dialog asking for user confirmation to group items into a new group. GroupItems will be merged.
     *
     * @param toGroup Set of items to group if user confirms.
     */
    private void groupDialog(List<Item> toGroup) {
        String title = null;
        for (Item item : toGroup) {
            if (item instanceof GroupItem) {
                title = ((GroupItem) item).getTitle();
                break;
            }
        }
        groupDialogScreen.open(screenPane, menagerie, settings, title, toGroup, group -> {
            Main.log.info("Created group: " + group);
            Platform.runLater(() -> {
                if (currentSearch.getResults().contains(group)) itemGridView.select(group, false, false);
            });
        });
    }

    /**
     * Opens the slideshow screen.
     *
     * @param items Set of items to show in the slideshow.
     */
    private void openSlideShow(List<Item> items) {
        items = new ArrayList<>(items);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) {
                GroupItem group = (GroupItem) items.remove(i);
                items.addAll(i, group.getElements());
            }
        }
        slideshowScreen.open(screenPane, menagerie, items);
    }

    /**
     * Opens a dialog asking for user confirmation to move files to a new folder. All files will be direct children of specified folder after moving.
     *
     * @param items Set of items to move. Groups will be expanded to include group elements.
     */
    private void moveFilesDialog(List<Item> items) {
        items = new ArrayList<>(items);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) {
                GroupItem group = (GroupItem) items.remove(i);
                items.addAll(i, group.getElements());
            }
        }
        List<Item> finalItems = items;

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Move files to folder...");
        String folder = settings.getString(Settings.Key.DEFAULT_FOLDER);
        if (folder != null && !folder.isEmpty()) dc.setInitialDirectory(new File(folder));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            ProgressScreen ps = new ProgressScreen();
            CancellableThread ct = new CancellableThread() {
                @Override
                public void run() {
                    final int total = finalItems.size();
                    int i = 0;

                    for (Item item : finalItems) {
                        if (!running) break;

                        i++;

                        if (item instanceof MediaItem) {
                            File f = result.toPath().resolve(((MediaItem) item).getFile().getName()).toFile();
                            if (!((MediaItem) item).getFile().equals(f)) {
                                File dest = MainController.resolveDuplicateFilename(f);

                                if (!((MediaItem) item).moveFile(dest)) {
                                    Main.log.severe("Failed to rename " + ((MediaItem) item).getFile() + " to " + dest);
                                }
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

    /**
     * Attempts to revert to the previous search.
     */
    private void explorerGoBack() {
        if (searchHistory.empty()) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            SearchHistory history = searchHistory.pop();

            listDescendingToggleButton.setSelected(history.isDescending());
            showGroupedToggleButton.setSelected(history.isShowGrouped());
            searchTextField.setText(history.getSearch());
            applySearch(history.getSearch(), history.getGroupScope(), history.isDescending(), history.isShowGrouped());
            searchHistory.pop(); // Pop history item that was JUST created by the new search.

            if (searchHistory.isEmpty()) backButton.setDisable(true);

            itemGridView.clearSelection();
            //            history.getSelected().forEach(item -> itemGridView.select(item, true, false));
            itemGridView.getSelected().addAll(history.getSelected());
            if (history.getSelected() != null && !history.getSelected().isEmpty()) {
                // Unselect and reselect last item
                itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true, false);
                itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true, false);
            }
        }
    }

    /**
     * Sets the search scope to a group search and applies an empty search.
     *
     * @param group Group scope.
     */
    private void explorerOpenGroup(GroupItem group) {
        searchTextField.setText(null);
        applySearch(searchTextField.getText(), group, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
    }

    /**
     * Parses a search string, applies the search, updates grid, registers search listeners, and previews first item.
     *
     * @param search      Search string to parse rules from.
     * @param descending  Order results in descending order.
     * @param showGrouped Show MediaItems that are in a group.
     */
    private void applySearch(String search, GroupItem groupScope, boolean descending, boolean showGrouped) {
        Main.log.info("Searching: \"" + search + "\", group:" + groupScope + ", descending:" + descending + ", showGrouped:" + showGrouped);

        // Clean up previous search
        if (currentSearch != null) {
            GroupItem scope = null;
            if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
            searchHistory.push(new SearchHistory(currentSearch.getSearchString(), scope, itemGridView.getSelected(), currentSearch.isDescending(), currentSearch.isShowGrouped()));
            backButton.setDisable(false);

            menagerie.unregisterSearch(currentSearch);
            currentSearch.getResults().removeListener(searchChangeListener);
        }
        previewItem(null);

        // Create new search
        if (groupScope == null) {
            currentSearch = new Search(search, descending, showGrouped);
            scopeLabel.setText("Scope: All");
        } else {
            currentSearch = new GroupSearch(search, groupScope, descending);
            scopeLabel.setText("Scope: " + groupScope.getTitle());
        }
        menagerie.registerSearch(currentSearch);
        currentSearch.refreshSearch(menagerie.getItems());
        currentSearch.getResults().addListener(searchChangeListener);

        itemGridView.clearSelection();
        itemGridView.getItems().clear();
        itemGridView.getItems().addAll(currentSearch.getResults());

        if (!itemGridView.getItems().isEmpty()) itemGridView.select(itemGridView.getItems().get(0), false, false);
    }

    /**
     * Sets the item grid width.
     *
     * @param n Width of grid in number of cells.
     */
    private void setGridWidth(int n) {
        final double width = 18 + (Thumbnail.THUMBNAIL_SIZE + ItemGridView.CELL_BORDER * 2 + itemGridView.getHorizontalCellSpacing() * 2) * n;
        itemGridView.setMinWidth(width);
        itemGridView.setMaxWidth(width);
        itemGridView.setPrefWidth(width);
    }

    /**
     * Parses a string and applies tag edits to currently selected items. Opens a confirmation dialog if 100 or more items will be modified by this operation.
     *
     * @param input Tag edit string.
     */
    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || itemGridView.getSelected().isEmpty()) return;
        lastEditTagString = input.trim();

        if (itemGridView.getSelected().size() < 100) {
            editTagsUtility(input);
        } else {
            new ConfirmationScreen().open(screenPane, "Editting large number of items", "You are attempting to edit " + itemGridView.getSelected().size() + " items. Continue?", () -> editTagsUtility(input), null);
        }
    }

    /**
     * Actual workhorse tag editing method. Parses tag edit string, makes changes, and verifies changed items against the search.
     *
     * @param input Tag edit string.
     */
    private void editTagsUtility(String input) {
        List<Item> changed = new ArrayList<>();
        Map<Item, List<Tag>> added = new HashMap<>();
        Map<Item, List<Tag>> removed = new HashMap<>();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    for (Item item : itemGridView.getSelected()) {
                        if (item.removeTag(t)) {
                            changed.add(item);

                            removed.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                        }
                    }
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (Item item : itemGridView.getSelected()) {
                    if (item.addTag(t)) {
                        changed.add(item);

                        added.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                    }
                }
            }
        }

        if (!changed.isEmpty()) {
            menagerie.refreshInSearches(changed);

            tagEditHistory.push(new TagEditEvent(added, removed));
        }
    }

    // ---------------------------------- Compute Utilities -----------------------------

    /**
     * Attempts to resolve a filename conflict caused by a pre-existing file at the same path. Appends an incremented number surrounded by parenthesis to the file if it already exists.
     *
     * @param file File to resolve name for.
     * @return File pointing to a file that does not exist yet.
     */
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

    /**
     * Starts a folder watcher thread. Kills an active folder watcher thread first, if present.
     *
     * @param folder        Target folder to watch for new files.
     * @param moveToDefault Move found files to default folder as specified by settings object.
     */
    private void startWatchingFolderForImages(String folder, boolean moveToDefault) {
        File watchFolder = new File(folder);
        if (watchFolder.exists() && watchFolder.isDirectory()) {
            Main.log.info("Starting folder watcher in folder: " + watchFolder);
            folderWatcherThread = new FolderWatcherThread(watchFolder, Filters.FILE_NAME_FILTER, 30000, files -> {
                for (File file : files) {
                    Main.log.info("Folder watcher got file to import: " + file);
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

                    importer.addJob(new ImportJob(file));
                }
            });
            folderWatcherThread.setDaemon(true);
            folderWatcherThread.start();
        }
    }

    /**
     * Cleanly exits the JFX application and releases all threads and resources.
     *
     * @param revertDatabase Revert database to last backup.
     */
    private void cleanExit(boolean revertDatabase) {
        Main.log.info("Attempting clean exit");

        previewMediaView.releaseVLCJ();
        slideshowScreen.releaseVLCJ();
        duplicateOptionsScreen.releaseVLCJ();
        Thumbnail.getVideoThumbnailThread().releaseResources();

        try {
            settings.save();
        } catch (IOException e1) {
            Main.log.log(Level.WARNING, "Failed to save settings to file", e1);
        }

        new Thread(() -> {
            try {
                Main.log.info("Attempting to shut down Menagerie database and defragment the file");
                menagerie.getDatabaseManager().shutdownDefrag();
                Main.log.info("Done defragging database file");

                if (revertDatabase) {
                    File database = DatabaseManager.resolveDatabaseFile(settings.getString(Settings.Key.DATABASE_URL));
                    File backup = new File(database + ".bak");
                    Main.log.warning(String.format("Reverting to last backup database: %s", backup.toString()));
                    try {
                        Files.move(backup.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Main.log.log(Level.SEVERE, "Failed to revert the database: " + database, e);
                    }
                }

                Main.log.info("Finished shutting down...");
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "SQL exception when shutting down with defrag", e);
            }
        }).start();

        Platform.exit();
    }

    // ---------------------------------- Action Event Handlers --------------------------

    public void searchButtonOnAction(ActionEvent event) {
        GroupItem scope = null;
        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
        itemGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        GroupItem scope = null;
        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected());
        itemGridView.requestFocus();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        screenPane.open(importDialogScreen);
        event.consume();
    }

    public void pruneFileLessMenuButtonOnAction(ActionEvent event) {
        ProgressScreen ps = new ProgressScreen();
        CancellableThread ct = new CancellableThread() {
            @Override
            public void run() {
                final int total = menagerie.getItems().size();
                int i = 0;

                List<Item> toDelete = new ArrayList<>();

                for (Item item : menagerie.getItems()) {
                    if (!running) break;
                    i++;

                    if (item instanceof MediaItem && !((MediaItem) item).getFile().exists()) {
                        toDelete.add(item);
                    }

                    final int finalI = i;
                    Platform.runLater(() -> ps.setProgress(finalI, total));
                }

                menagerie.forgetItems(toDelete);
                Platform.runLater(() -> {
                    ps.close();
                    new AlertDialogScreen().open(screenPane, "Pruning complete", toDelete.size() + " file-less items pruned.", null);
                });
            }
        };
        ps.open(screenPane, "Pruning Items", "Finding and pruning items that have become detached from their file...", ct::cancel);
        ct.setDaemon(true);
        ct.start();

        event.consume();
    }

    public void buildSimilarityCacheMenuButtonOnAction(ActionEvent event) {
        ProgressScreen ps = new ProgressScreen();
        CancellableThread ct = new CancellableThread() {
            @Override
            public void run() {
                final int total = menagerie.getItems().size();

                for (int i = 0; i < menagerie.getItems().size(); i++) {
                    if (!(menagerie.getItems().get(i) instanceof MediaItem)) continue;
                    MediaItem i1 = (MediaItem) menagerie.getItems().get(i);
                    if (i1.getHistogram() == null || i1.hasNoSimilar()) continue;

                    boolean hasSimilar = false;
                    for (int j = 0; j < menagerie.getItems().size(); j++) {
                        if (i == j) continue;
                        if (!(menagerie.getItems().get(j) instanceof MediaItem)) continue;
                        MediaItem i2 = (MediaItem) menagerie.getItems().get(j);
                        if (i2.getHistogram() == null || i2.hasNoSimilar()) continue;

                        double similarity = i1.getSimilarityTo(i2);
                        if (similarity >= MediaItem.MIN_CONFIDENCE) {
                            hasSimilar = true;
                            break;
                        }
                    }

                    if (i1.getId() == 48295) System.out.println(hasSimilar);

                    if (!hasSimilar) i1.setHasNoSimilar(true);

                    final int finalI = i;
                    Platform.runLater(() -> ps.setProgress(finalI, total));
                }

                Platform.runLater(ps::close);
            }
        };
        ps.open(screenPane, "Building similarity cache", "Caching items that have no possible similar items", ct::cancel);
        ct.setDaemon(true);
        ct.start();

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
        slideshowScreen.open(screenPane, menagerie, itemGridView.getSelected());
        event.consume();
    }

    public void viewSlideShowAllMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, menagerie.getItems());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        tagListScreen.open(screenPane, menagerie.getTags());
        event.consume();
    }

    public void revertDatabaseMenuButtonOnAction(ActionEvent event) {
        File database = DatabaseManager.resolveDatabaseFile(settings.getString(Settings.Key.DATABASE_URL));
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

    public void logButtonOnAction(ActionEvent event) {
        screenPane.open(logScreen);
        logButton.setStyle(null);
        event.consume();
    }

    public void backButtonOnAction(ActionEvent event) {
        explorerGoBack();
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
                    menagerie.getDatabaseManager().enqueue(() -> cleanExit(false));
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
                    screenPane.open(importDialogScreen);
                    event.consume();
                    break;
                case H:
                    screenPane.open(helpScreen);
                    event.consume();
                    break;
                case D:
                    duplicateOptionsScreen.open(screenPane, menagerie, itemGridView.getSelected(), currentSearch.getResults(), menagerie.getItems());
                    event.consume();
                    break;
                case Z:
                    if (tagEditHistory.empty()) {
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        TagEditEvent peek = tagEditHistory.peek();
                        new ConfirmationScreen().open(screenPane, "Undo last tag edit?", "Tags were added to " + peek.getAdded().keySet().size() + " items.\nTags were removed from " + peek.getRemoved().keySet().size() + " others.", () -> {
                            TagEditEvent pop = tagEditHistory.pop();
                            pop.revertAction();

                            List<Item> list = new ArrayList<>();
                            pop.getAdded().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            pop.getRemoved().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            menagerie.refreshInSearches(list);
                        }, null);
                    }
                    event.consume();
                    break;
                case N:
                    screenPane.open(importerScreen);
                    event.consume();
                    break;
                case L:
                    screenPane.open(logScreen);
                    logButton.setStyle(null);
                    event.consume();
                    break;
                default:
                    break;
            }
        } else {
            switch (event.getCode()) {
                case ESCAPE:
                    itemGridView.requestFocus();
                    event.consume();
                    break;
                case ALT:
                    event.consume(); // Workaround for alt-tabbing correctly
                    break;
                case ENTER:
                    if (itemGridView.getSelected().size() == 1 && itemGridView.getSelected().get(0) instanceof GroupItem) {
                        explorerOpenGroup((GroupItem) itemGridView.getSelected().get(0));
                    }
                    event.consume();
                    break;
                case BACK_SPACE:
                    explorerGoBack();
                    event.consume();
                    break;
            }
        }
    }

    public void explorerRootPaneOnKeyReleased(KeyEvent event) {
        // Workaround for alt-tabbing correctly
        if (event.getCode() == KeyCode.ALT) {
            if (menuBar.isFocused()) {
                itemGridView.requestFocus();
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
                itemGridView.requestFocus();
                event.consume();
                break;
            case ESCAPE:
                editTagsTextField.setText(null);
                itemGridView.requestFocus();
                event.consume();
                break;
            default:
                break;
        }
    }

}
